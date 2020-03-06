package com.csrapp.csr.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface JobDao {
    @Query("SELECT * FROM job WHERE stream=:stream")
    fun getJobsByStream(stream: String): List<JobEntity>

    @Query("SELECT * FROM job WHERE id=:id")
    fun getJobById(id: Int): List<JobEntity>

    @Query("SELECT * FROM job")
    fun getAllJobs(): List<JobEntity>
}