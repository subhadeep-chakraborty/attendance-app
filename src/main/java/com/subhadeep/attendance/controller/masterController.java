package com.subhadeep.attendance.controller;

import com.subhadeep.attendance.model.*;
import com.subhadeep.attendance.repository.*;
import com.subhadeep.attendance.service.AuthService;
import com.subhadeep.attendance.service.ClassService;
import com.subhadeep.attendance.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class masterController {

    @Autowired
    private EmailService emailService;
    @Autowired
    private VerificationTokenRepository verificationTokenRepo;
    @Autowired
    private GradeRepository gradeRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private ClassService classService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private SubjectRepository subjectRepo;
    @Autowired
    private AttendanceRepository attendanceRepo;


    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "timeout", required = false) String timeout,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "emailNotVerified", required = false) String emailNotVerified,
                            @RequestParam(value = "notApproved", required = false) String notApproved,
                            Model model) {
        if (timeout != null) model.addAttribute("timeout", true);
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        if ("true".equals(registered)) model.addAttribute("registrationSuccess", true);
        if ("false".equals(registered)) model.addAttribute("registrationError", true);
        if ("true".equals(error)) model.addAttribute("error", true);
        if ("true".equals(timeout)) model.addAttribute("timeout", true);
        if ("true".equals(logout)) model.addAttribute("logout", true);
        if ("true".equals(registered)) model.addAttribute("registrationSuccess", true);
        if ("false".equals(registered)) model.addAttribute("registrationError", true);
        if ("true".equals(emailNotVerified)) model.addAttribute("emailNotVerified", true);
        if ("true".equals(notApproved)) model.addAttribute("notApproved", true);
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
        session.setAttribute("name", user.getName());
        if (user == null) {
            return "redirect:/login?error=true";
        }

        if (!user.isEmailVerified()) {
            return "redirect:/login?emailNotVerified=true";
        }

        if (!user.isApproved()) {
            return "redirect:/login?notApproved=true";
        }


        session.setAttribute("email", user.getEmail());
        session.setAttribute("userEmail", user.getEmail());
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
        userRepo.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationTokenRepo.save(verificationToken);

        emailService.sendVerificationEmail(user, token);

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
            List<String> subjectNames = student.getEnrolledSubjects().stream()
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


    @PostMapping("/unenroll")
    public String unenrollStudent(@RequestParam String studentEmail,
                                  @RequestParam String subjectName) {
        User student = userRepo.findByEmail(studentEmail).orElse(null);
        Subject subject = subjectRepo.findByName(subjectName).orElse(null);
        if (student != null && subject != null) {
            student.getEnrolledSubjects().remove(subject);
            userRepo.save(student);
            return "redirect:/manage-students?success=true";
        }
        return "redirect:/manage-students?error=true";
    }

//    @PostMapping("/assign-class")
//    public String assignClassToStudent(@RequestParam String studentEmail,
//                                       @RequestParam Long subjectId) {
//        try {
//            Optional<User> studentOpt = userRepo.findByEmail(studentEmail);
//            Optional<Subject> subjectOpt = subjectRepo.findById(subjectId);
//
//
//            if (studentOpt.isPresent() && subjectOpt.isPresent()) {
//                User student = studentOpt.get();
//                Subject subject = subjectOpt.get();
//
//                student.getSubjects().add(subject);
//                userRepo.save(student);
//
//                return "redirect:/manage-students?success=true";
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "redirect:/manage-students?error=true";
//    }

    @PostMapping("/assign-class")
    public String assignClass(@RequestParam Long subjectId, @RequestParam String studentEmail) {
        User student = userRepo.findByEmail(studentEmail).orElse(null);
        Subject subject = subjectRepo.findById(subjectId).orElse(null);
        if (student != null && subject != null) {
            student.getEnrolledSubjects().add(subject);
            userRepo.save(student);
            return "redirect:/manage-students?success=true";
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
    public String student(HttpSession session, Model model) {
        if (!hasRole(session, "STUDENT")) {
            return "redirect:/login?timeout=true";
        }

        String email = (String) session.getAttribute("email");
        User student = authService.getUserByEmail(email);

        if (student == null) {
            return "redirect:/login?timeout=true";
        }
        model.addAttribute("studentName", student.getName());

        List<Attendance> records = attendanceRepo.findByStudent(student);
        long total = records.size();
        long presentCount = records.stream().filter(Attendance::isPresent).count();

        double percentage = total > 0 ? ((double) presentCount / total) * 100 : 0;
        model.addAttribute("attendancePercentage", String.format("%.2f", percentage));


        return "student-dashboard";
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard(HttpSession session, Model model) {
        if (!hasRole(session, "STUDENT")) {
            return "redirect:/login?timeout=true";
        }

        String email = (String) session.getAttribute("email");
        User student = authService.getUserByEmail(email);

        if (student == null) {
            return "redirect:/login?timeout=true";
        }
        model.addAttribute("studentName", student.getName());

        List<Attendance> records = attendanceRepo.findByStudent(student);
        long total = records.size();
        long presentCount = records.stream().filter(Attendance::isPresent).count();

        double percentage = total > 0 ? ((double) presentCount / total) * 100 : 0;
        model.addAttribute("attendancePercentage", String.format("%.2f", percentage));

        return "student-dashboard";
    }


    @GetMapping("/mark-attendance")
    public String markAttendance(HttpSession session, Model model) {
        if (!hasRole(session, "TEACHER")) {
            return "redirect:/login?timeout=true";
        }
        String email = (String) session.getAttribute("email");
        User teacher = userRepo.findByEmail(email).orElse(null);

        List<Subject> assignedSubjects = subjectRepo.findSubjectsByTeacher(teacher);

        model.addAttribute("subjects", assignedSubjects);
        model.addAttribute("students", new ArrayList<User>()); // initially empty
        return "mark-attendance";
    }



    @GetMapping("/get-students")
    @ResponseBody
    public List<Map<String, String>> getStudentsForSubject(@RequestParam Long subjectId) {
        Subject subject = subjectRepo.findById(subjectId).orElse(null);
        if (subject == null) return Collections.emptyList();

//        List<Map<String, String>> students = new ArrayList<>();
//        for (User student : subject.getEnrolledStudents()) {
//            Map<String, String> map = new HashMap<>();
//            map.put("email", student.getEmail());
//            map.put("name", student.getName());
//            students.add(map);
//        }
//        return students;
        List<User> students = subject.getEnrolledStudents(); // assuming this exists
        List<Map<String, String>> result = new ArrayList<>();

        for (User student : students) {
            Map<String, String> map = new HashMap<>();
            map.put("email", student.getEmail());
            map.put("name", student.getName());
            result.add(map);
        }

        return result;
    }



    @GetMapping("/record-grades")
    public String recordGrades(HttpSession session, Model model) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";

        String teacherEmail = (String) session.getAttribute("email");
        User teacher = userRepo.findById(teacherEmail).orElse(null);

        List<Subject> subjects = classService.getSubjectsByTeacherEmail(teacherEmail);
        model.addAttribute("subjects", subjects);

        List<User> students = userRepo.findByRole(Role.STUDENT);
        model.addAttribute("students", students);
        model.addAttribute("subjects", teacher.getSubjects());

        return "record-grades";
    }

    @PostMapping("/record-grades")
    public String saveGrades(@RequestParam Long subjectId,
                             @RequestParam Map<String, String> grades,
                             RedirectAttributes redirectAttributes) {

        Subject subject = subjectRepo.findById(subjectId).orElse(null);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid subject.");
            return "redirect:/record-grades";
        }

        for (String key : grades.keySet()) {
            if (key.startsWith("grades[")) {
                String email = key.substring(7, key.indexOf("]")); // extract email
                String fieldType = key.substring(key.indexOf("].") + 2); // grade or feedback

                User student = userRepo.findById(email).orElse(null);
                if (student == null) continue;

                Grade gradeRecord = gradeRepo.findByStudentAndSubject(student, subject)
                        .orElse(new Grade());

                gradeRecord.setStudent(student);
                gradeRecord.setSubject(subject);

                if ("grade".equals(fieldType)) {
                    gradeRecord.setGrade(grades.get(key));
                } else if ("feedback".equals(fieldType)) {
                    gradeRecord.setFeedback(grades.get(key));
                }

                gradeRepo.save(gradeRecord);
            }
        }

        redirectAttributes.addFlashAttribute("message", "Grades saved successfully.");
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
    public String viewAttendance(Model model, HttpSession session) {
        if (!hasRole(session, "STUDENT")) return "redirect:/login?timeout=true";

        String email = (String) session.getAttribute("email"); // assuming email is saved in session
        User student = userRepo.findById(email).orElse(null);

        if (student == null) {
            return "redirect:/login"; // or error page
        }

        List<Attendance> records = attendanceRepo.findByStudent(student);
        model.addAttribute("attendanceList", records);

        return "view-attendance";
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        VerificationToken verificationToken = verificationTokenRepo.findByToken(token);
        if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired token.");
            return "redirect:/login";
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);
        redirectAttributes.addFlashAttribute("message", "Email verified successfully.");
        return "redirect:/login";
    }


    @GetMapping("/view-grades")
    public String viewGrades(HttpSession session, Model model) {
        if (!hasRole(session, "STUDENT")) return "redirect:/login?timeout=true";
        String email = (String) session.getAttribute("email");
        List<Grade> grades = gradeRepo.findByStudent_Email(email);
        model.addAttribute("gradeList", grades);
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

    @PostMapping("/mark-attendance")
    public String saveAttendance(@RequestParam String date,
                                 @RequestParam(name = "class") String classId,
                                 @RequestParam Map<String, String> allParams,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        System.out.println("date: " + date);
        System.out.println("classId: " + classId);
        System.out.println("allParams: " + allParams);
        Subject subject = subjectRepo.findById(Long.valueOf(classId)).orElse(null);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid subject selected.");
            return "redirect:/mark-attendance";
        }
         if("undefined".equals(classId) || classId.isBlank()) {
            return "redirect:/mark-attendance?error=invalidClass";
        }

        LocalDate attendanceDate = LocalDate.parse(date);
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("status_")) {
                String studentEmail = entry.getKey().substring(7); // email instead of ID
                User student = userRepo.findById(studentEmail).orElse(null); // find by email, not Long ID

                if (student != null && !attendanceRepo.existsByStudentAndSubjectAndDate(student, subject, attendanceDate)) {
                    Attendance attendance = new Attendance();
                    attendance.setStudent(student);
                    attendance.setSubject(subject);
                    attendance.setDate(attendanceDate);
                    attendance.setStatus(AttendanceStatus.valueOf(entry.getValue().toUpperCase()));
                    attendance.setPresent(entry.getValue().equalsIgnoreCase("Present"));
                    attendanceRepo.save(attendance);
                }
            }
        }

        redirectAttributes.addFlashAttribute("message", "Attendance saved successfully!");
        return "redirect:/mark-attendance";
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
