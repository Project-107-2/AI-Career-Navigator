import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Считает статистику по вакансиям одной роли. Чистая логика, без сети. */
public class StatsService {

    public RoleStats compute(Role role, List<Vacancy> all) {
        // вакансии без единого навыка из справочника — обычно шум поиска, их не считаем
        List<Vacancy> vacancies = new ArrayList<>();
        for (Vacancy v : all) {
            if (!v.skills().isEmpty()) vacancies.add(v);
        }

        return new RoleStats(
                role.id(),
                vacancies.size(),
                medianSalary(vacancies),
                "RUR",
                skillFrequency(vacancies)
        );
    }

    /** Сколько раз встречается каждый навык, от частых к редким. */
    List<SkillCount> skillFrequency(List<Vacancy> vacancies) {
        Map<Skill, Integer> counts = new LinkedHashMap<>();
        for (Vacancy v : vacancies) {
            for (Skill s : v.skills()) {
                counts.merge(s, 1, Integer::sum);
            }
        }

        List<SkillCount> result = new ArrayList<>();
        for (Map.Entry<Skill, Integer> e : counts.entrySet()) {
            double share = (double) e.getValue() / vacancies.size();
            result.add(new SkillCount(e.getKey().id(), e.getKey().name(), e.getValue(), share));
        }
        result.sort(Comparator.comparingInt(SkillCount::count).reversed());
        return result;
    }

    /**
     * Медиана зарплаты. Для вакансии: есть «от» и «до» — берём среднее,
     * есть одно — берём его, нет ничего — вакансию пропускаем.
     */
    Integer medianSalary(List<Vacancy> vacancies) {
        List<Integer> salaries = new ArrayList<>();
        for (Vacancy v : vacancies) {
            Integer s = salaryOf(v);
            if (s != null) salaries.add(s);
        }
        if (salaries.isEmpty()) return null;

        Collections.sort(salaries);
        int n = salaries.size();
        if (n % 2 == 1) return salaries.get(n / 2);
        return (salaries.get(n / 2 - 1) + salaries.get(n / 2)) / 2;
    }

    private static Integer salaryOf(Vacancy v) {
        if (v.salaryFrom() != null && v.salaryTo() != null) return (v.salaryFrom() + v.salaryTo()) / 2;
        if (v.salaryFrom() != null) return v.salaryFrom();
        return v.salaryTo(); // может быть null
    }
}
