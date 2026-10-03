"""
Builds sample CSV files for the Exam Allocation System.

Usage:
    python generate_sample_data.py                      -> 20 branches x 25 = 500 students
    python generate_sample_data.py --per-branch 50      -> 1000 students
    python generate_sample_data.py --per-branch 250     -> 5000 students
    python generate_sample_data.py --per-branch 250 --check-only

Same seed gives the same data every time, so results are repeatable.
"""

import argparse
import csv
import os
import random
from collections import defaultdict
from datetime import date, timedelta

SEMESTERS = [1, 3, 5, 7]          # odd-semester exams
ADMISSION_YEAR = {1: 26, 3: 25, 5: 24, 7: 23}

# code, full name, family (shares base subjects), department that owns the courses
BRANCHES = [
    ("CSE",  "Computer Science & Engineering",                  "CS",   "Computer Science"),
    ("AIML", "CSE (Artificial Intelligence & Machine Learning)", "CS",   "Computer Science"),
    ("CSDS", "CSE (Data Science)",                              "CS",   "Computer Science"),
    ("CSCY", "CSE (Cyber Security)",                            "CS",   "Computer Science"),
    ("CSCC", "CSE (Cloud Computing)",                           "CS",   "Computer Science"),
    ("ISE",  "Information Science & Engineering",               "CS",   "Computer Science"),
    ("IT",   "Information Technology",                          "CS",   "Computer Science"),
    ("AIDS", "Artificial Intelligence & Data Science",          "CS",   "Computer Science"),
    ("ECE",  "Electronics & Communication Engineering",         "EC",   "Electronics & Communication"),
    ("VLSI", "ECE (VLSI Design & Technology)",                  "EC",   "Electronics & Communication"),
    ("EIE",  "Electronics & Instrumentation Engineering",       "EC",   "Electronics & Communication"),
    ("EEE",  "Electrical & Electronics Engineering",            "EE",   "Electrical Engineering"),
    ("ME",   "Mechanical Engineering",                          "ME",   "Mechanical Engineering"),
    ("MCT",  "Mechatronics",                                    "ME",   "Mechanical Engineering"),
    ("RAA",  "Robotics & Automation",                           "ME",   "Mechanical Engineering"),
    ("AUTO", "Automobile Engineering",                          "ME",   "Mechanical Engineering"),
    ("AERO", "Aerospace Engineering",                           "ME",   "Aerospace Engineering"),
    ("CIV",  "Civil Engineering",                               "CIV",  "Civil Engineering"),
    ("BT",   "Biotechnology",                                   "LIFE", "Life Sciences"),
    ("CHE",  "Chemical Engineering",                            "LIFE", "Life Sciences"),
]

# two shared subjects per semester for every branch in a family
BASE_SUBJECTS = {
    "CS":   {3: ["Data Structures and Algorithms", "Discrete Mathematical Structures"],
             5: ["Database Management Systems", "Operating Systems"],
             7: ["Software Engineering", "Computer Networks"]},
    "EC":   {3: ["Network Analysis", "Analog Electronics"],
             5: ["Digital Signal Processing", "Microcontrollers and Embedded Systems"],
             7: ["Digital Communication", "Antennas and Wave Propagation"]},
    "EE":   {3: ["Electric Circuit Analysis", "Electrical Machines I"],
             5: ["Power Systems I", "Control Systems"],
             7: ["Power Electronics", "Switchgear and Protection"]},
    "ME":   {3: ["Engineering Thermodynamics", "Mechanics of Materials"],
             5: ["Fluid Mechanics", "Machine Design"],
             7: ["Heat and Mass Transfer", "Manufacturing Technology"]},
    "CIV":  {3: ["Strength of Materials", "Surveying"],
             5: ["Structural Analysis", "Geotechnical Engineering"],
             7: ["Design of RC Structures", "Transportation Engineering"]},
    "LIFE": {3: ["Biochemistry", "Chemical Process Calculations"],
             5: ["Bioprocess Principles", "Chemical Reaction Engineering"],
             7: ["Process Control", "Plant Design and Economics"]},
}

# one specialisation subject per semester, unique to each branch
SPECIAL_SUBJECTS = {
    "CSE":  ["Object Oriented Programming with Java", "Theory of Computation", "Compiler Design"],
    "AIML": ["Foundations of Machine Learning", "Deep Learning", "Natural Language Processing"],
    "CSDS": ["Statistics for Data Science", "Big Data Analytics", "Data Visualization"],
    "CSCY": ["Cryptography Fundamentals", "Network Security", "Ethical Hacking and Digital Forensics"],
    "CSCC": ["Cloud Infrastructure Basics", "Virtualization and Containers", "Cloud Security and DevOps"],
    "ISE":  ["Java Programming", "Web Technologies", "Information Security"],
    "IT":   ["Python Programming", "Full Stack Development", "Mobile Application Development"],
    "AIDS": ["Probability for AI", "Machine Learning Algorithms", "Generative AI Systems"],
    "ECE":  ["Signals and Systems", "Electromagnetic Theory", "Wireless Communication"],
    "VLSI": ["Digital System Design", "CMOS Circuit Design", "Physical Design Automation"],
    "EIE":  ["Sensors and Transducers", "Process Instrumentation", "Industrial Automation"],
    "EEE":  ["Analog and Digital Circuits", "Electrical Machines II", "Renewable Energy Systems"],
    "ME":   ["Kinematics of Machinery", "Dynamics of Machinery", "Industrial Engineering"],
    "MCT":  ["Sensors and Actuators", "Mechatronic System Design", "Industrial Robotics"],
    "RAA":  ["Robot Kinematics", "Control of Robotic Systems", "Machine Vision"],
    "AUTO": ["Automotive Engines", "Vehicle Dynamics", "Electric Vehicle Technology"],
    "AERO": ["Aerodynamics", "Aircraft Structures", "Propulsion Systems"],
    "CIV":  ["Building Materials and Construction", "Hydrology and Water Resources", "Environmental Engineering"],
    "BT":   ["Cell and Molecular Biology", "Genetic Engineering", "Bioinformatics"],
    "CHE":  ["Chemical Technology", "Mass Transfer Operations", "Petroleum Refining"],
}

COMMON_SUBJECTS = {
    1: [("MAT101", "Engineering Mathematics I"), ("PHY101", "Engineering Physics"),
        ("CHY101", "Engineering Chemistry"), ("PRG101", "Programming for Problem Solving"),
        ("ENG101", "Technical Communication")],
    3: [("MAT301", "Engineering Mathematics III")],
    5: [("HSS501", "Professional Ethics and Management")],
    7: [("MGT701", "Engineering Economics and Finance")],
}

OPEN_ELECTIVES = {
    3: ["Entrepreneurship Fundamentals", "Indian Constitution and Governance", "Financial Literacy",
        "Introduction to Psychology", "Environmental Sustainability", "Public Speaking"],
    5: ["Intellectual Property Rights", "Digital Marketing", "Disaster Management",
        "Introduction to Artificial Intelligence", "Renewable Energy Basics", "Cyber Laws"],
    7: ["Project Management", "Operations Research Basics", "Startup Finance",
        "Human Resource Management", "Technical Writing", "Supply Chain Basics"],
}

FACULTY_PER_DEPARTMENT = {
    "Computer Science": 40, "Electronics & Communication": 25, "Electrical Engineering": 15,
    "Mechanical Engineering": 25, "Aerospace Engineering": 10, "Civil Engineering": 15,
    "Life Sciences": 15, "Basic Sciences & Humanities": 25,
}

# hall block, how many halls, rows, columns
HALL_BLOCKS = [("A", 12, 8, 8), ("B", 12, 8, 8), ("C", 12, 6, 8), ("D", 12, 6, 8), ("E", 16, 5, 6)]

FIRST_NAMES = [
    "Aarav", "Aditya", "Akash", "Ananya", "Anjali", "Arjun", "Bhavana", "Chaitra", "Darshan", "Deepika",
    "Divya", "Gagan", "Harsha", "Ishita", "Karthik", "Kavya", "Keerthi", "Lakshmi", "Madhu", "Manoj",
    "Meghana", "Nandini", "Naveen", "Nikhil", "Pooja", "Prajwal", "Pranav", "Priya", "Rahul", "Rakshita",
    "Rohan", "Sahana", "Sanjay", "Shreya", "Shruti", "Siddharth", "Spoorthi", "Suhas", "Sumanth", "Tejas",
    "Varun", "Vidya", "Vikram", "Vinay", "Yash", "Zoya", "Mohammed", "Fatima", "Rehan", "Ayesha",
    "Abhishek", "Swathi", "Pavan", "Ramya", "Dhanush", "Sneha", "Kiran", "Amrutha", "Rishab", "Tanvi",
]
LAST_NAMES = [
    "Gowda", "Reddy", "Nair", "Sharma", "Kattimani", "Patil", "Hegde", "Rao", "Shetty", "Bhat",
    "Naik", "Kulkarni", "Joshi", "Iyer", "Menon", "Verma", "Gupta", "Singh", "Desai", "Pai",
    "Kamath", "Acharya", "Murthy", "Prasad", "Swamy", "Khan", "Ahmed", "Das", "Banerjee", "Chauhan",
    "Hiremath", "Angadi", "Biradar", "Jadhav", "Kumar", "Shenoy", "Poojary", "Lingaiah", "Yadav", "Mishra",
]


def make_person_name(rng):
    return f"{rng.choice(FIRST_NAMES)} {rng.choice(LAST_NAMES)}"


def build_faculty(rng):
    rows = []
    for department, how_many in FACULTY_PER_DEPARTMENT.items():
        for _ in range(how_many):
            rows.append([f"F{len(rows) + 1:03d}", "Dr. " + make_person_name(rng), department])
    return rows


def build_halls():
    rows = []
    for block, count, total_rows, total_columns in HALL_BLOCKS:
        for number in range(1, count + 1):
            rows.append([f"{block}-{number:02d}", total_rows, total_columns])
    return rows


def build_exam_slots(first_day=date(2026, 12, 1), working_days=14):
    rows = []
    day = first_day
    counted = 0
    while counted < working_days:
        if day.weekday() != 6:                      # skip Sundays
            rows.append([day.isoformat(), "FN"])
            rows.append([day.isoformat(), "AN"])
            counted += 1
        day += timedelta(days=1)
    return rows


def build_courses(faculty_rows):
    """Returns the course list plus lookups the enrollment step needs."""
    faculty_by_department = defaultdict(list)
    for code, _, department in faculty_rows:
        faculty_by_department[department].append(code)
    next_owner = defaultdict(int)

    def pick_owner(department):
        owners = faculty_by_department[department]
        owner = owners[next_owner[department] % len(owners)]
        next_owner[department] += 1
        return owner

    courses = []                                   # code, title, semester, faculty_code
    core_by_branch_sem = defaultdict(list)         # (branch_code, sem) -> course codes
    common_by_sem = defaultdict(list)
    open_electives_by_sem = defaultdict(list)
    seen_base = {}

    for sem, subjects in COMMON_SUBJECTS.items():
        for code, title in subjects:
            courses.append([code, title, sem, pick_owner("Basic Sciences & Humanities")])
            common_by_sem[sem].append(code)

    for sem, titles in OPEN_ELECTIVES.items():
        for number, title in enumerate(titles, start=1):
            code = f"OE{sem}{number:02d}"
            owner_department = list(FACULTY_PER_DEPARTMENT)[(number + sem) % len(FACULTY_PER_DEPARTMENT)]
            courses.append([code, title, sem, pick_owner(owner_department)])
            open_electives_by_sem[sem].append(code)

    for branch_code, full_name, family, department in BRANCHES:
        intro_code = f"{branch_code}101"
        courses.append([intro_code, f"Introduction to {full_name}", 1, pick_owner(department)])
        core_by_branch_sem[(branch_code, 1)].append(intro_code)

        for index, sem in enumerate([3, 5, 7]):
            for number, title in enumerate(BASE_SUBJECTS[family][sem], start=1):
                code = f"{family}{sem}{number:02d}"
                if code not in seen_base:                  # shared across the family
                    courses.append([code, title, sem, pick_owner(department)])
                    seen_base[code] = True
                core_by_branch_sem[(branch_code, sem)].append(code)
            special_code = f"{branch_code}{sem}01"
            courses.append([special_code, SPECIAL_SUBJECTS[branch_code][index], sem, pick_owner(department)])
            core_by_branch_sem[(branch_code, sem)].append(special_code)

    return courses, core_by_branch_sem, common_by_sem, open_electives_by_sem


def build_students_and_enrollments(per_branch, rng, core_by_branch_sem, common_by_sem, open_electives_by_sem):
    students = []
    enrollments = []

    for branch_code, full_name, _, _ in BRANCHES:
        for index in range(per_branch):
            sem = SEMESTERS[index % len(SEMESTERS)]
            reg_no = f"PU{ADMISSION_YEAR[sem]}{branch_code}{index + 1:03d}"
            students.append([reg_no, make_person_name(rng), full_name, sem])

            taken = list(common_by_sem[sem]) + list(core_by_branch_sem[(branch_code, sem)])
            if sem > 1:
                taken.append(rng.choice(open_electives_by_sem[sem]))

            # about 4% carry a backlog from an earlier semester
            if sem > 1 and rng.random() < 0.04:
                earlier = []
                for old_sem in SEMESTERS:
                    if old_sem < sem:
                        earlier += core_by_branch_sem[(branch_code, old_sem)] + common_by_sem[old_sem]
                backlog = rng.choice(earlier)
                if backlog not in taken:
                    taken.append(backlog)

            for course_code in taken:
                enrollments.append([reg_no, course_code])

    return students, enrollments


def write_csv(path, header, rows):
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(header)
        writer.writerows(rows)


def check_feasibility(enrollments, slot_count, hall_rows):
    """Runs the same idea the timetable generator will use, to see if the data fits."""
    students_in_course = defaultdict(set)
    courses_of_student = defaultdict(set)
    for reg_no, course_code in enrollments:
        students_in_course[course_code].add(reg_no)
        courses_of_student[reg_no].add(course_code)

    clashes = defaultdict(set)
    for course_set in courses_of_student.values():
        for course_a in course_set:
            clashes[course_a] |= course_set - {course_a}

    order = sorted(students_in_course, key=lambda c: (-len(clashes[c]), -len(students_in_course[c])))
    hall_capacity = sum(rows * cols for _, rows, cols in hall_rows)

    def colour(pick_slot):
        slot_of = {}
        load = [0] * slot_count
        for course in order:
            blocked = {slot_of[n] for n in clashes[course] if n in slot_of}
            free = [s for s in range(slot_count) if s not in blocked]
            if not free:
                return None, None
            chosen = pick_slot(free, load)
            slot_of[course] = chosen
            load[chosen] += len(students_in_course[course])
        return slot_of, load

    first_fit, first_fit_load = colour(lambda free, load: free[0])
    balanced, balanced_load = colour(lambda free, load: min(free, key=lambda s: load[s]))

    print(f"  students: {len(courses_of_student)}, courses: {len(students_in_course)}, hall capacity: {hall_capacity}")
    for label, slot_of, load in (("first free slot", first_fit, first_fit_load),
                                 ("least loaded free slot", balanced, balanced_load)):
        if slot_of is None:
            print(f"  {label}: NOT ENOUGH SLOTS")
            continue
        used = len({s for s in slot_of.values()})
        print(f"  {label}: slots used {used}/{slot_count}, busiest slot {max(load)} students "
              f"({'fits' if max(load) <= hall_capacity else 'EXCEEDS'} hall capacity), "
              f"invigilators needed in busiest slot about {-(-max(load) // 30)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--per-branch", type=int, default=25)
    parser.add_argument("--out", default=os.path.dirname(os.path.abspath(__file__)))
    parser.add_argument("--seed", type=int, default=2026)
    parser.add_argument("--check-only", action="store_true")
    args = parser.parse_args()

    rng = random.Random(args.seed)
    faculty = build_faculty(rng)
    halls = build_halls()
    slots = build_exam_slots()
    courses, core_map, common_map, oe_map = build_courses(faculty)
    students, enrollments = build_students_and_enrollments(args.per_branch, rng, core_map, common_map, oe_map)

    print(f"{len(BRANCHES)} branches x {args.per_branch} = {len(students)} students")
    check_feasibility(enrollments, len(slots), halls)
    if args.check_only:
        return

    os.makedirs(args.out, exist_ok=True)
    write_csv(os.path.join(args.out, "students.csv"), ["reg_no", "name", "branch", "semester"], students)
    write_csv(os.path.join(args.out, "enrollments.csv"), ["reg_no", "course_code"], enrollments)
    write_csv(os.path.join(args.out, "courses.csv"), ["course_code", "title", "semester", "faculty_code"], courses)
    write_csv(os.path.join(args.out, "faculty.csv"), ["employee_code", "name", "department"], faculty)
    write_csv(os.path.join(args.out, "halls.csv"), ["hall_name", "total_rows", "total_columns"], halls)
    write_csv(os.path.join(args.out, "exam_slots.csv"), ["exam_date", "session"], slots)
    print(f"files written to {args.out}")


if __name__ == "__main__":
    main()
