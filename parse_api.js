const fs = require('fs');
const path = require('path');

function findJavaFiles(dir, fileList = []) {
    if (!fs.existsSync(dir)) return fileList;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            findJavaFiles(filePath, fileList);
        } else if (filePath.endsWith('Controller.java')) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

const javaFiles = findJavaFiles('e:/workspaces/BML/bml-backend');
let markdown = '# 项目接口完整说明 (Project APIs)\\n\\n';
let totalApis = 0;

for (const file of javaFiles) {
    const content = fs.readFileSync(file, 'utf8');
    
    // Check if it's a RestController or Controller
    if (!content.includes('@RestController') && !content.includes('@Controller')) continue;
    
    // Extract Tag
    const tagMatch = content.match(/@Tag\(\s*name\s*=\s*"([^"]+)"(?:.*?description\s*=\s*"([^"]+)")?/);
    const apiMatch = content.match(/@Api\([\s\S]*?tags\s*=\s*\{?"([^"]+)"\}?/);
    const javadocClassMatch = content.match(/\/\*\*[\s\S]*?\*\s+([^\n]+)[\s\S]*?\*\//);
    
    let className = path.basename(file, '.java');
    let moduleName = className;
    let moduleDesc = '';
    
    if (tagMatch) {
        moduleName = tagMatch[1];
        moduleDesc = tagMatch[2] || '';
    } else if (apiMatch) {
        moduleName = apiMatch[1];
    } else if (javadocClassMatch) {
        moduleName = javadocClassMatch[1].replace(/\*/g, '').trim();
    }

    // Extract RequestMapping
    const rmMatch = content.match(/@RequestMapping\(\s*(?:value\s*=)?\s*"([^"]+)"/);
    let basePath = rmMatch ? rmMatch[1] : '';
    if (basePath.endsWith('/')) basePath = basePath.slice(0, -1);
    if (basePath && !basePath.startsWith('/')) basePath = '/' + basePath;

    let tableContent = '';
    
    const lines = content.split('\n');
    let currentSummary = '';
    let currentDesc = '';
    let inJavadoc = false;
    let javadocLines = [];
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();
        
        // simple javadoc capture
        if (line.startsWith('/**')) {
            inJavadoc = true;
            javadocLines = [];
        } else if (inJavadoc && line.startsWith('*') && !line.includes('*/')) {
            let docStr = line.replace(/^\*\s*/, '').trim();
            if (docStr && !docStr.startsWith('@')) {
                javadocLines.push(docStr);
            }
        } else if (line.includes('*/')) {
            inJavadoc = false;
            // First line of javadoc is often the summary
            if (javadocLines.length > 0 && !currentSummary) {
                currentSummary = javadocLines[0];
            }
        }
        
        let opMatch = line.match(/@Operation\(\s*summary\s*=\s*"([^"]+)"(?:.*?description\s*=\s*"([^"]+)")?/);
        let apiOpMatch = line.match(/@ApiOperation\(\s*(?:value\s*=)?\s*"([^"]+)"/);
        
        if (opMatch) {
            currentSummary = opMatch[1];
            currentDesc = opMatch[2] || '';
        } else if (apiOpMatch) {
            currentSummary = apiOpMatch[1];
        }
        
        // Find simple mappings
        let mapMatch = line.match(/@(Get|Post|Put|Delete)Mapping\(\s*(?:value\s*=)?\s*"([^"]*)"/);
        // Sometimes it's @GetMapping without value
        if (!mapMatch) {
             mapMatch = line.match(/@(Get|Post|Put|Delete)Mapping/);
             if (mapMatch) mapMatch = [mapMatch[0], mapMatch[1], ""];
        }
        
        if (mapMatch) {
            let method = mapMatch[1].toUpperCase();
            let subPath = mapMatch[2] || '';
            if (subPath && !subPath.startsWith('/')) subPath = '/' + subPath;
            let fullPath = basePath + subPath;
            
            // Clean up
            let cSum = currentSummary || '-';
            let cDesc = currentDesc || '-';
            
            tableContent += `| ${cSum} | ${cDesc} | \`${method}\` | \`${fullPath}\` |\n`;
            totalApis++;
            
            // reset
            currentSummary = '';
            currentDesc = '';
            javadocLines = [];
        }
    }
    
    if (tableContent !== '') {
        markdown += `## ${moduleName} (${className})\n`;
        if (moduleDesc) markdown += `> ${moduleDesc}\n\n`;
        markdown += `**基础路径 (Base Path)**: \`${basePath || '/'}\`\n\n`;
        markdown += `| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |\n`;
        markdown += `| --- | --- | --- | --- |\n`;
        markdown += tableContent + '\n';
    }
}

markdown = `共分析到 **${totalApis}** 个API接口。\n` + markdown;

fs.writeFileSync('e:/workspaces/BML/api_docs.md', markdown);
console.log('Total APIs found: ' + totalApis);
