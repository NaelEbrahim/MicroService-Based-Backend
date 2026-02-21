package com.example.course_server.controller;

import com.example.course_server.dto.CourseDTO;
import com.example.course_server.service.CourseService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;


    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CourseDTO dto) {
        return courseService.createCourse(dto);
    }


    @PutMapping("update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CourseDTO dto) {
        return courseService.updateCourse(id, dto);
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return courseService.deleteCourse(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable("id") Integer id){
        return courseService.getCourseById(id);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return courseService.getAllCourses();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer id) {
        return courseService.approveCourse(id);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Integer id) {
        return courseService.rejectCourse(id);
    }

}
