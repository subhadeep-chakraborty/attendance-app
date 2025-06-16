package com.subhadeep.attendance.controller;

import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class masterController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "timeout", required = false) String timeout,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (timeout != null) model.addAttribute("timeout", true);
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        return "login";
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

        return switch (user.getRole()) {
            case "ADMIN" -> "redirect:/admin";
            case "TEACHER" -> "redirect:/teacher";
            case "STUDENT" -> "redirect:/student";
            default -> "redirect:/login";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session) {
        if (!hasRole(session, "ADMIN")) {
            return "redirect:/login?timeout=true";
        }
        return "admin-dashboard";
    }

    @GetMapping("/teacher")
    public String teacher(HttpSession session) {
        if (!hasRole(session, "TEACHER")) {
            return "redirect:/login?timeout=true";
        }
        return "teacher-dashboard";
    }

    @GetMapping("/student")
    public String student(HttpSession session) {
        if (!hasRole(session, "STUDENT")) {
            return "redirect:/login?timeout=true";
        }
        return "student-dashboard";
    }

    @GetMapping("/mark-attendance")
    public String markAttendance(HttpSession session) {
        if (!hasRole(session, "TEACHER")) {
            return "redirect:/login?timeout=true";
        }
        return "mark-attendance";
    }

    @GetMapping("/record-grades")
    public String recordGrades(HttpSession session) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";
        return "record-grades";
    }

    @GetMapping("/user-approval")
    public String userApproval(HttpSession session) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";
        return "user-approval";
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
    public String manageClasses(HttpSession session) {
        if (!hasRole(session, "ADMIN")) return "redirect:/login?timeout=true";
        return "manage-classes";
    }

    @GetMapping("/generate-reports")
    public String generateReports(HttpSession session) {
        if (!hasRole(session, "TEACHER")) return "redirect:/login?timeout=true";
        return "generate-reports";
    }

    private boolean hasRole(HttpSession session, String requiredRole) {
        String email = (String) session.getAttribute("email");
        String role = (String) session.getAttribute("role");
        return email != null && role != null && role.equalsIgnoreCase(requiredRole);
    }
}
