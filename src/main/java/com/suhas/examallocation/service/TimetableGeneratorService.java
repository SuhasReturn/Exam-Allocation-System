package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughSlotsException;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Assigns each course to an exam slot such that no student has
 * two exams in the same slot.
 *
 * This is the graph coloring problem:
 * - Each course is a NODE.
 * - An EDGE connects two courses if at least one student is enrolled in both.
 * - Each exam slot is a COLOR.
 * - We need to assign a color (slot) to every node (course) so that
 *   no two connected nodes share the same color.
 *
 * We use a greedy algorithm: sort courses by how many clashes they have
 * (most constrained first), then assign each the earliest slot that
 * doesn't conflict with its neighbors. This is the "largest degree first"
 * heuristic — it doesn't guarantee the minimum number of slots, but it
 * works well in practice and is simple to understand.
 */
@Service
public class TimetableGeneratorService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamSlotRepository examSlotRepository;
    private final ExamRepository examRepository;

    public TimetableGeneratorService(CourseRepository courseRepository,
                                     EnrollmentRepository enrollmentRepository,
                                     ExamSlotRepository examSlotRepository,
                                     ExamRepository examRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examSlotRepository = examSlotRepository;
        this.examRepository = examRepository;
    }

    /**
     * Generates the full timetable in one transaction.
     * If anything fails, all exam records are rolled back.
     *
     * @param startDate the first date exams can be scheduled on
     * @param maxSlotsPerDay 1 or 2 (FN only, or FN+AN)
     */
    @Transactional
    public List<Exam> generateTimetable(LocalDate startDate, int maxSlotsPerDay) {
        List<Course> allCourses = courseRepository.findAll();
        if (allCourses.isEmpty()) {
            throw new IllegalArgumentException("No courses found — import courses before generating the timetable");
        }

        // clear any previously generated timetable
        examRepository.deleteAll();

        Map<Long, Set<Long>> clashMap = buildClashMap(allCourses);
        List<Course> sortedCourses = sortByClashCountDescending(allCourses, clashMap);
        List<ExamSlot> slots = createSlots(startDate, maxSlotsPerDay, sortedCourses.size());

        // courseId -> assigned slot index
        Map<Long, Integer> courseSlotAssignment = new HashMap<>();
        int maxSlotUsed = -1;

        for (Course course : sortedCourses) {
            int slotIndex = findFirstFreeSlot(course.getId(), clashMap, courseSlotAssignment, slots.size());

            if (slotIndex == -1) {
                int needed = countExtraSlotsNeeded(sortedCourses, clashMap, courseSlotAssignment, slots.size());
                throw new NotEnoughSlotsException(
                        "Not enough exam slots. Need at least " + needed
                        + " more slot(s) to schedule all courses without clashes");
            }

            courseSlotAssignment.put(course.getId(), slotIndex);
            if (slotIndex > maxSlotUsed) {
                maxSlotUsed = slotIndex;
            }
        }

        List<Exam> exams = new ArrayList<>();
        for (Course course : allCourses) {
            int slotIndex = courseSlotAssignment.get(course.getId());
            ExamSlot slot = slots.get(slotIndex);
            Exam exam = new Exam(course, slot);
            exams.add(examRepository.save(exam));
        }

        return exams;
    }

    /**
     * Builds the clash graph: for each course, the set of course IDs
     * it conflicts with.
     *
     * Two courses clash if they share at least one student.
     * We find this by iterating over all students and connecting
     * every pair of courses each student is enrolled in.
     */
    Map<Long, Set<Long>> buildClashMap(List<Course> courses) {
        Map<Long, Set<Long>> clashMap = new HashMap<>();
        for (Course course : courses) {
            clashMap.put(course.getId(), new HashSet<>());
        }

        // collect all students who are enrolled in more than one course
        Set<Long> allStudentIds = new HashSet<>();
        Map<Long, List<Long>> studentCourses = new HashMap<>();

        for (Course course : courses) {
            List<Long> studentIds = enrollmentRepository.findStudentIdsByCourseId(course.getId());
            for (Long studentId : studentIds) {
                allStudentIds.add(studentId);
                studentCourses.computeIfAbsent(studentId, k -> new ArrayList<>())
                              .add(course.getId());
            }
        }

        // for each student enrolled in multiple courses, add edges between all pairs
        for (Long studentId : allStudentIds) {
            List<Long> courseIds = studentCourses.get(studentId);
            if (courseIds.size() < 2) {
                continue;
            }
            for (int i = 0; i < courseIds.size(); i++) {
                for (int j = i + 1; j < courseIds.size(); j++) {
                    Long courseA = courseIds.get(i);
                    Long courseB = courseIds.get(j);
                    clashMap.get(courseA).add(courseB);
                    clashMap.get(courseB).add(courseA);
                }
            }
        }

        return clashMap;
    }

    private List<Course> sortByClashCountDescending(List<Course> courses,
                                                     Map<Long, Set<Long>> clashMap) {
        List<Course> sorted = new ArrayList<>(courses);
        sorted.sort((a, b) -> {
            int clashA = clashMap.getOrDefault(a.getId(), Collections.emptySet()).size();
            int clashB = clashMap.getOrDefault(b.getId(), Collections.emptySet()).size();
            return Integer.compare(clashB, clashA);
        });
        return sorted;
    }

    /**
     * Finds the first slot index that doesn't conflict with any
     * already-assigned neighbor of this course in the clash graph.
     * Returns -1 if no slot is free.
     */
    private int findFirstFreeSlot(Long courseId,
                                   Map<Long, Set<Long>> clashMap,
                                   Map<Long, Integer> courseSlotAssignment,
                                   int totalSlots) {
        Set<Long> clashingCourses = clashMap.getOrDefault(courseId, Collections.emptySet());
        Set<Integer> usedSlots = new HashSet<>();

        for (Long clashingCourseId : clashingCourses) {
            Integer assignedSlot = courseSlotAssignment.get(clashingCourseId);
            if (assignedSlot != null) {
                usedSlots.add(assignedSlot);
            }
        }

        for (int i = 0; i < totalSlots; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Creates exam slots starting from startDate.
     * Each day gets maxSlotsPerDay slots (FN, then AN if maxSlotsPerDay=2).
     * We create enough slots to cover the worst case (one slot per course).
     */
    private List<ExamSlot> createSlots(LocalDate startDate, int maxSlotsPerDay, int courseCount) {
        List<ExamSlot> slots = new ArrayList<>();
        LocalDate currentDate = startDate;
        Session[] sessions = maxSlotsPerDay == 1
                ? new Session[]{Session.FN}
                : new Session[]{Session.FN, Session.AN};

        while (slots.size() < courseCount) {
            for (Session session : sessions) {
                if (slots.size() >= courseCount) {
                    break;
                }
                LocalDate slotDate = currentDate;
                Session slotSession = session;
                ExamSlot slot = examSlotRepository.findByExamDateAndSession(slotDate, slotSession)
                        .orElseGet(() -> examSlotRepository.save(new ExamSlot(slotDate, slotSession)));
                slots.add(slot);
            }
            currentDate = currentDate.plusDays(1);
        }

        return slots;
    }

    /**
     * When we run out of slots, count how many extra slots would be needed.
     * Does a dry run of remaining unassigned courses.
     */
    private int countExtraSlotsNeeded(List<Course> sortedCourses,
                                      Map<Long, Set<Long>> clashMap,
                                      Map<Long, Integer> partialAssignment,
                                      int currentSlotCount) {
        Map<Long, Integer> trialAssignment = new HashMap<>(partialAssignment);
        int maxSlotNeeded = currentSlotCount - 1;

        for (Course course : sortedCourses) {
            if (trialAssignment.containsKey(course.getId())) {
                continue;
            }

            Set<Long> clashingCourses = clashMap.getOrDefault(course.getId(), Collections.emptySet());
            Set<Integer> usedSlots = new HashSet<>();
            for (Long clashId : clashingCourses) {
                Integer assignedSlot = trialAssignment.get(clashId);
                if (assignedSlot != null) {
                    usedSlots.add(assignedSlot);
                }
            }

            int slotIndex = 0;
            while (usedSlots.contains(slotIndex)) {
                slotIndex++;
            }

            trialAssignment.put(course.getId(), slotIndex);
            if (slotIndex > maxSlotNeeded) {
                maxSlotNeeded = slotIndex;
            }
        }

        return (maxSlotNeeded + 1) - currentSlotCount;
    }
}
