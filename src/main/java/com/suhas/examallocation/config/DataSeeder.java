package com.suhas.examallocation.config;

import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.model.Hall;
import com.suhas.examallocation.model.Role;
import com.suhas.examallocation.model.Student;
import com.suhas.examallocation.model.UserAccount;
import com.suhas.examallocation.repository.FacultyRepository;
import com.suhas.examallocation.repository.HallRepository;
import com.suhas.examallocation.repository.StudentRepository;
import com.suhas.examallocation.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds baseline records on first application startup:
 * 1. Default admin account (admin / admin123)
 * 2. Examination halls (LH-101, LH-102, LH-201)
 * 3. Faculty members (CSE, ISE, ECE, MAT departments)
 * 4. Sample faculty login (faculty1 / faculty123)
 * 5. Sample student record & login (student1 / student123)
 *
 * Runs once after Spring finishes booting. All checks are idempotent.
 */
@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedInitialData(UserAccountRepository userAccountRepository,
                                             HallRepository hallRepository,
                                             FacultyRepository facultyRepository,
                                             StudentRepository studentRepository,
                                             PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Admin
            if (!userAccountRepository.existsByUsername("admin")) {
                UserAccount admin = new UserAccount(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        Role.ADMIN);
                userAccountRepository.save(admin);
                System.out.println("Default admin account created: username 'admin', password 'admin123'");
            }

            // 2. Seed Halls if none exist
            if (hallRepository.count() == 0) {
                hallRepository.save(new Hall("LH-101", 6, 4)); // 24 seats
                hallRepository.save(new Hall("LH-102", 6, 4)); // 24 seats
                hallRepository.save(new Hall("LH-201", 5, 4)); // 20 seats
                System.out.println("Seeded 3 examination halls (LH-101, LH-102, LH-201)");
            }

            // 3. Seed Faculty if none exist
            Faculty facultyRao = null;
            if (facultyRepository.count() == 0) {
                facultyRao = facultyRepository.save(new Faculty("Dr. Rajesh Rao", "Computer Science and Engineering"));
                facultyRepository.save(new Faculty("Prof. Anita Desai", "Information Science and Engineering"));
                facultyRepository.save(new Faculty("Dr. Vikram Singh", "Electronics and Communication Engineering"));
                facultyRepository.save(new Faculty("Prof. Priya Sharma", "Mathematics"));
                System.out.println("Seeded 4 faculty members");
            } else {
                facultyRao = facultyRepository.findAll().get(0);
            }

            // 4. Seed Faculty Account if none exists
            if (!userAccountRepository.existsByUsername("faculty1") && facultyRao != null) {
                UserAccount facultyUser = new UserAccount(
                        "faculty1",
                        passwordEncoder.encode("faculty123"),
                        Role.FACULTY);
                facultyUser.setLinkedFaculty(facultyRao);
                userAccountRepository.save(facultyUser);
                System.out.println("Default faculty account created: username 'faculty1', password 'faculty123'");
            }

            // 5. Seed initial student if none exist and link student1 account
            if (!userAccountRepository.existsByUsername("student1")) {
                Student student = studentRepository.findByRegNo("1PU21CS001").orElseGet(() -> {
                    Student newStudent = new Student("1PU21CS001", "Aarav Sharma", "CSE", 5);
                    return studentRepository.save(newStudent);
                });

                UserAccount studentUser = new UserAccount(
                        "student1",
                        passwordEncoder.encode("student123"),
                        Role.STUDENT);
                studentUser.setLinkedStudent(student);
                userAccountRepository.save(studentUser);
                System.out.println("Default student account created: username 'student1', password 'student123'");
            }
        };
    }
}
