import type {
  GovernanceFieldPriority,
  GovernanceFormFieldSchema,
  GovernanceFormSectionSchema
} from '../types/governance';

/**
 * 按字段优先级拆分治理表单分区。
 * 用于将“常用条件常显、次要条件折叠”的布局能力沉到通用层，
 * 后续其他查询页只需要在 schema 中标记字段优先级即可复用。
 */
export function splitGovernanceSectionsByPriority(sections: GovernanceFormSectionSchema[]) {
  const primarySections = buildSectionsByPriority(sections, 'primary');
  const secondarySections = buildSectionsByPriority(sections, 'secondary');

  return {
    primarySections,
    secondarySections,
    primaryFieldCount: countFields(primarySections),
    secondaryFieldCount: countFields(secondarySections)
  };
}

function buildSectionsByPriority(
  sections: GovernanceFormSectionSchema[],
  priority: GovernanceFieldPriority
) {
  return sections.reduce<GovernanceFormSectionSchema[]>((result, section) => {
      const fields = section.fields.filter(field => resolveFieldPriority(field) === priority);

      if (!fields.length) {
        return result;
      }

      result.push({
        ...section,
        fields,
        columns: normalizeColumns(section.columns, fields.length)
      });

      return result;
    }, []);
}

function resolveFieldPriority(field: GovernanceFormFieldSchema) {
  return field.priority || 'primary';
}

function normalizeColumns(columns: GovernanceFormSectionSchema['columns'], fieldCount: number) {
  const safeColumns = columns || 2;
  return Math.max(1, Math.min(fieldCount, safeColumns)) as GovernanceFormSectionSchema['columns'];
}

function countFields(sections: GovernanceFormSectionSchema[]) {
  return sections.reduce((sum, section) => sum + section.fields.length, 0);
}
