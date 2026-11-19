package org.career.navigator.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Словарь синонимов навыков: связывает разные написания (EN/RU, сокращения, версии)
 * с каноническим skillId и предкомпилирует регулярные выражения для поиска в вакансиях.
 */
public class SynonymDictionary {

  private final Map<Long, List<String>> synonymsBySkillId = new LinkedHashMap<>();
  private final Map<String, Long> exactLookup = new HashMap<>();
  private final Map<Long, Pattern> compiledPatterns = new LinkedHashMap<>();

  public void register(Long skillId, String canonicalName, List<String> aliases) {
    Set<String> allForms = new LinkedHashSet<>();
    allForms.add(canonicalName);
    if (aliases != null) {
      allForms.addAll(aliases);
    }

    List<String> list = List.copyOf(allForms);
    synonymsBySkillId.put(skillId, list);

    StringBuilder combinedRegex = new StringBuilder();
    for (String term : list) {
      String normalized = normalizeKey(term);
      if (!normalized.isEmpty()) {
        exactLookup.put(normalized, skillId);
      }

      if (!combinedRegex.isEmpty()) {
        combinedRegex.append('|');
      }
      combinedRegex.append(buildTermRegex(term));
    }

    compiledPatterns.put(
        skillId,
        Pattern.compile(
            combinedRegex.toString(),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
  }

  /** Точная нормализация термина (например, "постгрес" или "k8s" -> skillId). */
  public Optional<Long> resolve(String rawSkillName) {
    if (rawSkillName == null || rawSkillName.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(exactLookup.get(normalizeKey(rawSkillName)));
  }

  /** Ищет уникальные skillId всех навыков, упомянутых в тексте вакансии. */
  public Set<Long> findSkillIdsIn(String text) {
    Set<Long> found = new LinkedHashSet<>();
    if (text == null || text.isBlank()) {
      return found;
    }
    for (Map.Entry<Long, Pattern> entry : compiledPatterns.entrySet()) {
      if (entry.getValue().matcher(text).find()) {
        found.add(entry.getKey());
      }
    }
    return found;
  }

  public List<String> getSynonyms(Long skillId) {
    return synonymsBySkillId.getOrDefault(skillId, Collections.emptyList());
  }

  public Map<Long, List<String>> allMappings() {
    return Collections.unmodifiableMap(synonymsBySkillId);
  }

  private static String normalizeKey(String raw) {
    return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  /**
   * Границы слова с учётом спецсимволов (C++, C#, .NET, Node.js, CI/CD):
   * - точка в leftBoundary не даёт "js" срабатывать внутри "node.js" или "vue.js";
   * - для терминов на '+' или '#' разрешаем цифры справа (C++17, C++20).
   */
  private static String buildTermRegex(String term) {
    String trimmed = term.trim();
    String quoted = Pattern.quote(trimmed);
    String leftBoundary = "(?<![\\p{L}\\p{N}_+#.])";
    char lastChar = trimmed.charAt(trimmed.length() - 1);
    String rightBoundary =
        (lastChar == '+' || lastChar == '#')
            ? "(?![\\p{L}_+#])"
            : "(?![\\p{L}\\p{N}_+#])";
    return leftBoundary + quoted + rightBoundary;
  }
}
