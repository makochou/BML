const fs = require('fs');
const path = require('path');

const srcDir = 'e:/workspaces/BML/bml-backend';

// Helper to find all files
function findFiles(dir, suffix, fileList = []) {
    if (!fs.existsSync(dir)) return fileList;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            findFiles(filePath, suffix, fileList);
        } else if (filePath.endsWith(suffix)) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

const javaFiles = findFiles(srcDir, '.java');

// Build a map of class name -> file path to resolve DTOs
const classMap = {};
for (const f of javaFiles) {
    const className = path.basename(f, '.java');
    classMap[className] = f;
}

function parseDtoFields(className) {
    const file = classMap[className];
    if (!file) return [];
    
    const content = fs.readFileSync(file, 'utf8');
    const lines = content.split('\n');
    let fields = [];
    
    let currentDesc = '';
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();
        
        let schemaMatch = line.match(/@Schema\(\s*description\s*=\s*"([^"]+)"/);
        if (schemaMatch) {
            currentDesc = schemaMatch[1];
        } else if (line.match(/private\s+([A-Za-z0-9<>_]+)\s+([A-Za-z0-9_]+)\s*;/)) {
            let m = line.match(/private\s+([A-Za-z0-9<>_]+)\s+([A-Za-z0-9_]+)\s*;/);
            fields.push({
                type: m[1],
                name: m[2],
                desc: currentDesc || '-'
            });
            currentDesc = '';
        } else if (line.includes('/**') && line.includes('*/')) {
             let jdoc = line.match(/\/\*\*\s*(.*?)\s*\*\//);
             if (jdoc) currentDesc = jdoc[1];
        } else if (line.startsWith('/**')) {
            // multiline comment parse simplified
            let doc = '';
            let j = i + 1;
            while (j < lines.length && !lines[j].includes('*/')) {
                doc += lines[j].replace(/^\*\s*/, '').trim() + ' ';
                j++;
            }
            if (doc.trim()) currentDesc = doc.trim();
            i = j;
        }
    }
    return fields;
}

const controllers = javaFiles.filter(f => f.endsWith('Controller.java'));
let markdown = '# 项目接口极度详细说明\n\n该文档通过代码静态分析生成，包含请求参数、数据模型(DTO)详情等信息。\n\n';
let totalApis = 0;

for (const file of controllers) {
    const content = fs.readFileSync(file, 'utf8');
    if (!content.includes('@RestController') && !content.includes('@Controller')) continue;
    
    // Tag
    const tagMatch = content.match(/@Tag\(\s*name\s*=\s*"([^"]+)"(?:.*?description\s*=\s*"([^"]+)")?/);
    let className = path.basename(file, '.java');
    let moduleName = tagMatch ? tagMatch[1] : className;
    
    const rmMatch = content.match(/@RequestMapping\(\s*(?:value\s*=)?\s*"([^"]+)"/);
    let basePath = rmMatch ? rmMatch[1] : '';
    if (basePath.endsWith('/')) basePath = basePath.slice(0, -1);
    if (basePath && !basePath.startsWith('/')) basePath = '/' + basePath;

    let methodsOutput = '';
    
    const methodRegex = /@(Get|Post|Put|Delete)Mapping\s*\(\s*(?:value\s*=)?\s*"([^"]*)"(?:[^)]*)\)[\s\S]*?public\s+(?:<[^>]+>\s+)?([A-Za-z0-9<>_]+)\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)/g;
    
    let m;
    while ((m = methodRegex.exec(content)) !== null) {
        totalApis++;
        let httpMethod = m[1].toUpperCase();
        let subPath = m[2] || '';
        if (subPath && !subPath.startsWith('/')) subPath = '/' + subPath;
        let fullPath = basePath + subPath;
        let returnType = m[3];
        let methodName = m[4];
        let paramsStr = m[5];
        
        // Find operation summary
        let preContent = content.substring(0, m.index);
        let summaryMatch = preContent.match(/@Operation\(\s*summary\s*=\s*"([^"]+)"/g);
        let currentSummary = summaryMatch ? summaryMatch[summaryMatch.length - 1].match(/"([^"]+)"/)[1] : '-';
        
        methodsOutput += `### ${currentSummary}\n`;
        methodsOutput += `- **请求路径**: \`${fullPath}\`\n`;
        methodsOutput += `- **请求方式**: \`${httpMethod}\`\n`;
        methodsOutput += `- **控制器方法**: \`${methodName}()\`\n`;
        methodsOutput += `- **返回类型**: \`${returnType}\`\n\n`;
        
        if (paramsStr.trim()) {
            methodsOutput += `**请求参数**:\n\n`;
            methodsOutput += `| 参数名 | 类型 | 注解 | 描述 |\n`;
            methodsOutput += `| --- | --- | --- | --- |\n`;
            
            let params = paramsStr.split(',').map(p => p.trim());
            for (let p of params) {
                // remove annotations like @Validated
                p = p.replace(/@Validated\s+/, '');
                let parts = p.match(/(?:(@[A-Za-z]+(?:\([^)]+\))?)\s+)?([A-Za-z0-9<>_?, ]+)\s+([A-Za-z0-9_]+)/);
                
                if (parts) {
                    let anno = parts[1] || '';
                    let pType = parts[2].trim();
                    let pName = parts[3];
                    methodsOutput += `| \`${pName}\` | \`${pType}\` | \`${anno}\` | - |\n`;
                    
                    // If it's a DTO, parse its fields
                    if (pType.endsWith('DTO') || pType.endsWith('VO') || pType.endsWith('Body')) {
                        let innerType = pType.replace(/<.*>/, ''); // remove generics for DTO parsing
                        let fields = parseDtoFields(innerType);
                        if (fields.length > 0) {
                            methodsOutput += `\n> **${innerType} 数据结构**:\n\n`;
                            methodsOutput += `| 字段 | 类型 | 说明 |\n| --- | --- | --- |\n`;
                            for (let f of fields) {
                                methodsOutput += `| \`${f.name}\` | \`${f.type}\` | ${f.desc} |\n`;
                            }
                            methodsOutput += `\n`;
                        }
                    }
                } else {
                     methodsOutput += `| - | \`${p}\` | - | 无法精确解析参数 |\n`;
                }
            }
        } else {
            methodsOutput += `**请求参数**: 无\n\n`;
        }
        methodsOutput += `---\n\n`;
    }
    
    if (methodsOutput) {
        markdown += `## ${moduleName} (${className})\n`;
        markdown += methodsOutput;
    }
}

// Write the detailed API doc
fs.writeFileSync('e:/workspaces/BML/api_docs_detailed.md', markdown);
console.log('Detailed extraction complete: ' + totalApis + ' APIs');
