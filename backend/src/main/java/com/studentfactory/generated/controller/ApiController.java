package com.studentfactory.generated.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfactory.generated.model.AuditLog;
import com.studentfactory.generated.model.Category;
import com.studentfactory.generated.model.Department;
import com.studentfactory.generated.model.ManagedRecord;
import com.studentfactory.generated.model.TransactionItem;
import com.studentfactory.generated.model.UserAccount;
import com.studentfactory.generated.repository.AuditLogRepository;
import com.studentfactory.generated.repository.CategoryRepository;
import com.studentfactory.generated.repository.DepartmentRepository;
import com.studentfactory.generated.repository.RecordRepository;
import com.studentfactory.generated.repository.RoleRepository;
import com.studentfactory.generated.repository.TransactionRepository;
import com.studentfactory.generated.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final ObjectMapper mapper;
  private final BCryptPasswordEncoder passwordEncoder;
  private final UserRepository users;
  private final RoleRepository roles;
  private final CategoryRepository categories;
  private final DepartmentRepository departments;
  private final RecordRepository records;
  private final TransactionRepository transactions;
  private final AuditLogRepository auditLogs;

  public ApiController(
    ObjectMapper mapper,
    BCryptPasswordEncoder passwordEncoder,
    UserRepository users,
    RoleRepository roles,
    CategoryRepository categories,
    DepartmentRepository departments,
    RecordRepository records,
    TransactionRepository transactions,
    AuditLogRepository auditLogs
  ) {
    this.mapper = mapper;
    this.passwordEncoder = passwordEncoder;
    this.users = users;
    this.roles = roles;
    this.categories = categories;
    this.departments = departments;
    this.records = records;
    this.transactions = transactions;
    this.auditLogs = auditLogs;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "cab-booking-management-system-api");
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(@RequestBody Map<String, String> body) {
    UserAccount user = users.findByEmail(body.getOrDefault("email", ""))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(body.getOrDefault("password", ""), user.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    String token = Base64.getEncoder().encodeToString((user.email + ":cab-booking-management-system").getBytes(StandardCharsets.UTF_8));
    return Map.of("accessToken", token, "user", sanitizeUser(user));
  }

  @GetMapping("/categories")
  public Object categories(@RequestParam(required = false) Integer page, @RequestParam(defaultValue = "10") Integer limit) {
    if (page == null) return categories.findAll();
    return paginate(categories.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100))));
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public Category createCategory(@RequestBody Category category) throws JsonProcessingException {
    Category saved = categories.save(category);
    writeAudit("create", "category", saved.id, saved);
    return saved;
  }

  @GetMapping("/departments")
  public Object departments(@RequestParam(required = false) Integer page, @RequestParam(defaultValue = "10") Integer limit) {
    if (page == null) return departments.findAll();
    return paginate(departments.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100))));
  }

  @PostMapping("/departments")
  @ResponseStatus(HttpStatus.CREATED)
  public Department createDepartment(@RequestBody Department department) throws JsonProcessingException {
    Department saved = departments.save(department);
    writeAudit("create", "department", saved.id, saved);
    return saved;
  }

  @GetMapping("/records")
  public Object records(@RequestParam(required = false) Integer page, @RequestParam(defaultValue = "10") Integer limit) {
    if (page == null) return records.findAll();
    return paginate(records.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100))));
  }

  @PostMapping("/records")
  @ResponseStatus(HttpStatus.CREATED)
  public ManagedRecord createRecord(@RequestBody ManagedRecord record) throws JsonProcessingException {
    ManagedRecord saved = records.save(record);
    writeAudit("create", "record", saved.id, saved);
    return saved;
  }

  @GetMapping("/transactions")
  public Object transactions(@RequestParam(required = false) Integer page, @RequestParam(defaultValue = "10") Integer limit) {
    if (page == null) return transactions.findAll();
    return paginate(transactions.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100))));
  }

  @PostMapping("/transactions")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionItem createTransaction(@RequestBody TransactionItem transaction) throws JsonProcessingException {
    TransactionItem saved = transactions.save(transaction);
    writeAudit("create", "transaction", saved.id, saved);
    return saved;
  }

  @GetMapping("/users")
  public List<Map<String, Object>> users() {
    return users.findAll().stream().map(this::sanitizeUser).toList();
  }

  @GetMapping("/roles")
  public Object roles() {
    return roles.findAll();
  }

  @GetMapping("/reports/summary")
  public Map<String, Long> summary() {
    return Map.of(
      "records", records.count(),
      "categories", categories.count(),
      "departments", departments.count(),
      "transactions", transactions.count()
    );
  }

  @GetMapping("/audit-logs")
  public Object auditLogs(@RequestParam(required = false) Integer page, @RequestParam(defaultValue = "10") Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    if (page == null) return auditLogs.findAll(sort);
    return paginate(auditLogs.findAll(PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(limit, 1), 100), sort)));
  }

  private Map<String, Object> sanitizeUser(UserAccount user) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", user.id);
    item.put("email", user.email);
    item.put("displayName", user.displayName);
    item.put("roleId", user.roleId);
    return item;
  }

  private Map<String, Object> paginate(org.springframework.data.domain.Page<?> page) {
    return Map.of(
      "data", page.getContent(),
      "meta", Map.of(
        "page", page.getNumber() + 1,
        "limit", page.getSize(),
        "total", page.getTotalElements(),
        "totalPages", Math.max(1, page.getTotalPages())
      )
    );
  }
@GetMapping("/")
public String home() {
    return "Cab Booking Backend is running successfully!";
}
  private void writeAudit(String action, String entity, String entityId, Object payload) throws JsonProcessingException {
    AuditLog log = new AuditLog();
    log.actor = "admin@example.com";
    log.action = action;
    log.entity = entity;
    log.entityId = entityId;
    log.payload = mapper.writeValueAsString(payload);
    auditLogs.save(log);
  }
}
