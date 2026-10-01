package com.example.studenttracker.data.network

import com.example.studenttracker.domain.model.UserRoleResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AddStudentRequest(
    val action: String = "addStudent",
    val rollNumber: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val batch: String,
    val year: String,
    val semester: String,
    val section: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String
)

data class UpdateStudentRequest(
    val revision: Int = 0,
    val action: String = "updateStudent",
    val rollNumber: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val batch: String,
    val year: String,
    val semester: String,
    val section: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String
)

data class DeleteStudentRequest(
    val revision: Int = 0,
    val action: String = "deleteStudent",
    val rollNumber: String
)

data class ApiResponse(
    val status: String,
    val message: String
)

data class Student(
    val revision: Int = 0,
    val rollNumber: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val batch: String,
    val year: String,
    val semester: String,
    val section: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String
)

data class StudentListResponse(
    val status: String,
    val students: List<Student>? = null,
    val message: String? = null
)

data class AddFacultyRequest(
    val action: String = "addFaculty",
    val employeeId: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val designation: String,
    val joiningDate: String
)

data class UpdateFacultyRequest(
    val revision: Int = 0,
    val action: String = "updateFaculty",
    val employeeId: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val designation: String,
    val joiningDate: String
)

data class DeleteFacultyRequest(
    val revision: Int = 0,
    val action: String = "deleteFaculty",
    val employeeId: String
)

data class Faculty(
    val revision: Int = 0,
    val employeeId: String,
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val designation: String,
    val joiningDate: String
)

data class FacultyListResponse(
    val status: String,
    val faculty: List<Faculty>? = null,
    val message: String? = null
)

data class AddSubjectRequest(
    val action: String = "addSubject",
    val subjectCode: String,
    val subjectName: String,
    val facultyId: String,
    val semester: String,
    val totalClasses: String
)

data class UpdateSubjectRequest(
    val revision: Int = 0,
    val action: String = "updateSubject",
    val subjectCode: String,
    val subjectName: String,
    val facultyId: String,
    val semester: String,
    val totalClasses: String
)

data class DeleteSubjectRequest(
    val revision: Int = 0,
    val action: String = "deleteSubject",
    val subjectCode: String
)

data class Subject(
    val revision: Int = 0,
    val subjectCode: String,
    val subjectName: String,
    val facultyId: String,
    val semester: String,
    val totalClasses: String
)

data class SubjectListResponse(
    val status: String,
    val subjects: List<Subject>? = null,
    val message: String? = null
)

data class AttendanceReportItem(
    val studentId: String,
    val name: String,
    val totalPresent: String,
    val totalClasses: String,
    val percentage: String
)

data class AttendanceReportResponse(
    val status: String,
    val report: List<AttendanceReportItem>? = null,
    val message: String? = null
)

interface GoogleSheetsApi {
    @GET("exec")
    suspend fun getStudent(@Query("rollNumber") rollNumber: String, @Query("action") action: String = "getStudent"): StudentResponse
    @GET("exec")
    suspend fun getFacultyMember(@Query("employeeId") employeeId: String, @Query("action") action: String = "getFacultyMember"): FacultyResponse
    @GET("exec")
    suspend fun getSubject(@Query("subjectCode") subjectCode: String, @Query("action") action: String = "getSubject"): SubjectResponse
    @GET("exec")
    suspend fun getDashboard(@Query("action") action: String = "getDashboard"): DashboardResponse
    @GET("exec")
    suspend fun getRoster(@Query("subjectCode") subjectCode: String, @Query("action") action: String = "getRoster"): RosterResponse
    @POST("exec")
    suspend fun saveEnrollment(@Body request: EnrollmentRequest): ApiResponse
    @POST("exec")
    suspend fun saveAttendance(@Body request: AttendanceRequest): ApiResponse
    @GET("exec")
    suspend fun getAssessments(@Query("subjectCode") subjectCode: String, @Query("action") action: String = "getAssessments"): AssessmentsResponse
    @POST("exec")
    suspend fun saveAssessment(@Body request: AssessmentRequest): AssessmentResponse
    @GET("exec")
    suspend fun getMyProgress(@Query("action") action: String = "getMyProgress"): ProgressResponse
    @GET("exec")
    suspend fun getUserRole(@Query("action") action: String = "getUserRole"): UserRoleResponse

    @GET("exec")
    suspend fun getStudents(@Query("action") action: String = "getStudents"): StudentListResponse

    @POST("exec")
    suspend fun addStudent(@Body request: AddStudentRequest): ApiResponse

    @POST("exec")
    suspend fun updateStudent(@Body request: UpdateStudentRequest): ApiResponse

    @POST("exec")
    suspend fun deleteStudent(@Body request: DeleteStudentRequest): ApiResponse

    @GET("exec")
    suspend fun getFaculty(@Query("action") action: String = "getFaculty"): FacultyListResponse

    @POST("exec")
    suspend fun addFaculty(@Body request: AddFacultyRequest): ApiResponse

    @POST("exec")
    suspend fun updateFaculty(@Body request: UpdateFacultyRequest): ApiResponse

    @POST("exec")
    suspend fun deleteFaculty(@Body request: DeleteFacultyRequest): ApiResponse

    @GET("exec")
    suspend fun getSubjects(@Query("action") action: String = "getSubjects"): SubjectListResponse

    @POST("exec")
    suspend fun addSubject(@Body request: AddSubjectRequest): ApiResponse

    @POST("exec")
    suspend fun updateSubject(@Body request: UpdateSubjectRequest): ApiResponse

    @POST("exec")
    suspend fun deleteSubject(@Body request: DeleteSubjectRequest): ApiResponse

    @GET("exec")
    suspend fun getAttendanceReport(
        @Query("action") action: String = "getAttendanceReport",
        @Query("subjectCode") subjectCode: String
    ): AttendanceReportResponse
}
