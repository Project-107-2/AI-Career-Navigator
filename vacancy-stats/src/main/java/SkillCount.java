/** Как часто навык встречается в вакансиях роли. share — доля от 0 до 1. */
public record SkillCount(Long skillId, String name, int count, double share) {}
