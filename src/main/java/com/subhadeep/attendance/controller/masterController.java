package com.subhadeep.attendance.controller;

import com.subhadeep.attendance.model.Role;
import com.subhadeep.attendance.model.Subject;
import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.repository.SubjectRepository;
import com.subhadeep.attendance.repository.UserRepository;
import com.subhadeep.attendance.service.AuthService;
import com.subhadeep.attendance.service.ClassService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class masterController {

    @Autowired
    private AuthService authService;
    @Autowired
    private ClassService classService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private SubjectRepository subjectRepo;


    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "timeout", required = false) String timeout,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (timeout != null) model.addAttribute("timeout", true);
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        if ("true".equals(registered)) model.addAttribute("registrationSuccess", true);
        if ("false".equals(registered)) model.addAttribute("registrationError", true);
        return "login";
    }

    @GetMapping("/registration")
    public String showRegistrationPage() {
        return "registration";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session) {
        User user = authService.authenticate(email, password);
        if (user == null) {
            return "redirect:/login?error=true";
        }

        session.setAttribute("email", user.getEmail());
        session.setAttribute("role", user.getRole());
        session.setAttribute("name", user.getName());

        Role role = user.getRole();

        return switch (role != null ? role : Role.STUDENT) {
            case ADMIN -> "redirect:/admin";
            case TEACHER -> "redirect:/teacher";
            case STUDENT -> "redirect:/student";
        };
    }

    @PostMapping("/register")
    public String handleRegistration(@RequestParam String name,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String role,
                                     @RequestParam(required = false) String department,
                                     Model model) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password); // Hash later
        user.setRole(Role.valueOf(role.toUpperCase()));
        user.setDepartment(role.equalsIgnoreCase("TEACHER") ? department : null);

        boolean success = authService.register(user);

        if (!success) {
            return "redirect:/login?registered=false"; // Redirect with error
        }

        return "redirect:/login?registered=true"; // Redirect with success
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        if (!hasRole(session, "ADMIN")) {
            return "redirect:/login?timeout=true";
        }

        long activeUsers = authService.countApprovedUsers();
        long pendingUsers = authService.countPendingApprovals();

        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("pendingUsers", pendingUsers);

        return "admin-dashboard";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";

        // Add any required attributes to model
        return "admin-dashboard"; // this should match the .html file in templates
    }

    @GetMapping("/manage-students")
    public String manageStudents(Model model, @RequestParam(required = false) String success,
                                 @RequestParam(required = false) String error) {
        List<User> students = userRepo.findByRole(Role.STUDENT);
        List<Subject> subjects = subjectRepo.findAll();

        // Map of student -> list of subject names
        Map<User, List<String>> assignedMap = new LinkedHashMap<>();
        for (User student : students) {
            List<String> subjectNames = student.getSubjects().stream()
                    .map(Subject::getName)
                    .collect(Collectors.toList());
            assignedMap.put(student, subjectNames);
        }

        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("assigned", assignedMap);
        model.addAttribute("success", success != null);
        model.addAttribute("error", error != null);
        return "manage-students";
    }

    @PostMapping("/assign-class")
    public String assignClassToStudent(@RequestParam String studentEmail,
                                       @RequestParam Long subjectId) {
        try {
            Optional<User> studentOpt = userRepo.findByEmail(studentEmail);
            Optional<Subject> subjectOpt = subjectRepo.findById(subjectId);

            if (studentOpt.isPresent() && subjectOpt.isPresent()) {
                User student = studentOpt.get();
                Subject subject = subjectOpt.get();

                student.getSubjects().add(subject);
                userRepo.save(student);

                return "redirect:/manage-students?success=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/manage-students?error=true";
    }




    @GetMapping("/teacher")
    public String teacher(HttpSession session) {
        if (!hasRole(session, "TEACHER")) {
            return "redirect:/login?timeout=true";
        }
        return "redirect:/teacher-dashboard";
    }

    @GetMapping("/teacher-dashboard")
    public String teacherDashboard(HttpSession session, Model model) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";

        String email = (String) session.getAttribute("email");
        String name = (String) session.getAttribute("name");
        model.addAttribute("teacherName", name);

        List<String> classList = classService.getSubjectsByTeacherEmail(email)
                .stream()
                .map(Subject::getName)
                .toList();
        model.addAttribute("classList", classList);

        return "teacher-dashboard";
    }


    @GetMapping("/student")
    public String student(HttpSession session) {
        if (!hasRole(session, "STUDENT")) {
            return "redirect:/login?timeout=true";
        }
        return "student-dashboard";
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard(HttpSession session, Model model) {
        if (!hasRole(session, "STUDENT")) {
            return "redirect:/login?timeout=true";
        }

        String email = (String) session.getAttribute("email");
        User student = authService.getUserByEmail(email);
        model.addAttribute("studentName", student.getName());

        return "student-dashboard"; // Must match HTML filename
    }


    @GetMapping("/mark-attendance")
    public String markAttendance(HttpSession session) {
        if (!hasRole(session, "TEACHER")) {
            return "redirect:/login?timeout=true";
        }
        return "mark-attendance";
    }

    @GetMapping("/record-grades")
    public String recordGrades(HttpSession session, Model model) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";

        String teacherEmail = (String) session.getAttribute("email");

        List<Subject> subjects = classService.getSubjectsByTeacherEmail(teacherEmail);
        model.addAttribute("subjects", subjects);

        List<User> students = userRepo.findByRole(Role.STUDENT);
        model.addAttribute("students", students);

        return "record-grades";
    }

    @PostMapping("/record-grades")
    public String saveGrades(@RequestParam Long subjectId,
                             @RequestParam Map<String, String> grades,
                             HttpSession session, RedirectAttributes redirectAttributes) {

        grades.forEach((key, value) -> {
            if (key.startsWith("grades[")) {
                String email = key.substring(7, key.indexOf("]"));
                String field = key.substring(key.indexOf("].") + 2);

                // Extract or store grades and feedback using a map or DTO
                System.out.println("Email: " + email + ", Field: " + field + ", Value: " + value);
            }
        });

        redirectAttributes.addFlashAttribute("message", "Grades recorded successfully!");
        return "redirect:/record-grades";
    }

    @GetMapping("/user-approval")
    public String userApproval(HttpSession session, Model model) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";

        List<User> pendingUsers = authService.getPendingUsers();
        model.addAttribute("pendingUsers", pendingUsers);
        List<User> allUsers = authService.getAllUsers();  // not just pending
        model.addAttribute("allUsers", allUsers);

        return "user-approval";
    }

    @PostMapping("/approve-user")
    public String approveUser(@RequestParam String email) {
        authService.setUserApproval(email, true);
        return "redirect:/user-approval";
    }

    @PostMapping("/deactivate-user")
    public String deactivateUser(@RequestParam String email) {
        authService.deactivateUser(email);
        return "redirect:/user-approval";
    }


    @GetMapping("/view-attendance")
    public String viewAttendance(HttpSession session) {
        if (!hasRole(session, "STUDENT")) return "redirect:/login?timeout=true";
        return "view-attendance";
    }

    @GetMapping("/view-grades")
    public String viewGrades(HttpSession session) {
        if (!hasRole(session, "STUDENT")) return "redirect:/login?timeout=true";
        return "view-grades";
    }


    @GetMapping("/system-log")
    public String systemLog(HttpSession session) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";
        return "system-log";
    }


    @GetMapping("/manage-classes")
    public String manageClasses(HttpSession session, Model model) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";

        model.addAttribute("subjects", classService.getAllSubjects());

        return "manage-classes";
    }

    @GetMapping("/add-class")
    public String showAddClassForm(Model model) {
        model.addAttribute("teachers", classService.getAllTeachers());
        return "add-class";
    }

    @PostMapping("/add-class")
    public String handleAddClass(@RequestParam String name,
                                 @RequestParam List<String> teachers) {
        classService.addSubject(name, teachers);
        return "redirect:/manage-classes";
    }

    @PostMapping("/deactivate-class")
    public String deactivateClass(@RequestParam Long id) {
        classService.deactivateSubject(id);
        return "redirect:/manage-classes";
    }

    @PostMapping("/activate-class")
    public String activateClass(@RequestParam Long id) {
        classService.activateSubject(id);
        return "redirect:/manage-classes";
    }


    @GetMapping("/generate-reports")
    public String generateReports(HttpSession session) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";
        return "generate-report";
    }

    private boolean hasRole(HttpSession session, String requiredRole) {
        String email = (String) session.getAttribute("email");
        Object roleObj = session.getAttribute("role");

        if (email == null || roleObj == null) return false;

        if (roleObj instanceof Role role) {
            return role.name().equalsIgnoreCase(requiredRole);
        }

        return false;
    }
}
