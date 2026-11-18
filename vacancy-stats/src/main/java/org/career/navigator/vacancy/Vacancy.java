package org.career.navigator.vacancy;

import org.career.navigator.model.Skill;

import java.util.List;

/**
 * Одна вакансия. Навыки — общие объекты Skill, чтобы статистика
 * и расчёт соответствия у Алёны работали с одними и теми же навыками.
 * Зарплата может отсутствовать, поэтому Integer, а не int.
 */
public record Vacancy(
        String id,
        String name,
        List<Skill> skills,
        Integer salaryFrom,
        Integer salaryTo,
        String currency
) {}
