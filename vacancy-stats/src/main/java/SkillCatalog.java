import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Справочник навыков (пока в памяти).
 * У trudvsem нет готового списка навыков, как key_skills у hh.ru,
 * поэтому ищем названия навыков в тексте вакансии.
 */
public class SkillCatalog {
    private final List<Skill> skills = List.of(
            new Skill(1L, "Java", "Язык программирования Java"),
            new Skill(2L, "Spring", "Фреймворк Spring / Spring Boot"),
            new Skill(3L, "SQL", "Язык запросов к БД"),
            new Skill(4L, "PostgreSQL", "СУБД PostgreSQL"),
            new Skill(5L, "Hibernate", "ORM Hibernate"),
            new Skill(6L, "Git", "Система контроля версий"),
            new Skill(7L, "Docker", "Контейнеризация"),
            new Skill(8L, "Maven", "Сборщик Maven"),
            new Skill(9L, "Gradle", "Сборщик Gradle"),
            new Skill(10L, "Kotlin", "Язык Kotlin"),
            new Skill(11L, "Linux", "ОС Linux"),
            new Skill(12L, "REST", "REST API"),
            new Skill(13L, "Kafka", "Брокер сообщений Kafka"),
            new Skill(14L, "JUnit", "Тестирование JUnit")
    );

    public List<Skill> all() {
        return skills;
    }

    /** Какие навыки из справочника упоминаются в тексте (без учёта регистра). */
    public List<Skill> findIn(String text) {
        List<Skill> found = new ArrayList<>();
        if (text == null) return found;
        for (Skill s : skills) {
            // слово целиком: "Java" не должна находиться внутри "JavaScript"
            String regex = "(?<![\\p{L}\\p{N}])" + Pattern.quote(s.name()) + "(?![\\p{L}\\p{N}])";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                found.add(s);
            }
        }
        return found;
    }
}
