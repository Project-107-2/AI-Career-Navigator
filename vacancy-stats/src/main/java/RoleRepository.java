import java.util.HashMap;
import java.util.Map;

/** Роли в памяти. Пока одна, как в плане: «сначала одна роль, зато рабочая». */
public class RoleRepository {
    private final Map<Long, Role> roles = new HashMap<>();

    public RoleRepository() {
        add(new Role(1L, "Java-разработчик", "Backend-разработка на Java"));
    }

    public void add(Role role) {
        roles.put(role.id(), role);
    }

    /** null, если роли нет. */
    public Role findById(Long id) {
        return roles.get(id);
    }
}
