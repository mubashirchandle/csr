package com.csrapp.csr.utils

import android.content.Context
import com.csrapp.csr.data.*
import com.csrapp.csr.ui.taketest.aptitudetest.AptitudeTestViewModelFactory
import com.csrapp.csr.ui.taketest.personalitytest.PersonalityTestViewModelFactory
import com.csrapp.csr.ui.taketest.result.ResultViewModelFactory
import com.csrapp.csr.ui.viewstreams.jobdetail.JobDetailViewModelFactory
import com.csrapp.csr.ui.viewstreams.jobselection.JobSelectionViewModelFactory
import com.csrapp.csr.ui.viewstreams.streamselection.StreamSelectionViewModelFactory

object InjectorUtils {

    fun provideAptitudeTestViewModelFactory(context: Context): AptitudeTestViewModelFactory {
        val repository = AptitudeQuestionRepository.getInstance(
            CSRDatabase.getInstance(context.applicationContext).aptitudeQuestionDao()
        )
        return AptitudeTestViewModelFactory(repository)
    }

    fun providePersonalityTestViewModelFactory(context: Context): PersonalityTestViewModelFactory {
        val repository = PersonalityQuestionRepository.getInstance(
            CSRDatabase.getInstance(context.applicationContext).personalityQuestionDao()
        )
        return PersonalityTestViewModelFactory(repository)
    }

    fun provideResultViewModelFactory(context: Context): ResultViewModelFactory {
        val repository =
            ResultRepository.getInstance(CSRDatabase.getInstance(context.applicationContext).resultDao())
        return ResultViewModelFactory(repository)
    }

    fun provideStreamSelectionViewModelFactory(context: Context): StreamSelectionViewModelFactory {
        val repository =
            StreamRepository.getInstance(CSRDatabase.getInstance(context.applicationContext).streamDao())
        return StreamSelectionViewModelFactory(repository)
    }

    fun provideJobSelectionViewModelFactory(context: Context): JobSelectionViewModelFactory {
        val repository =
            JobRepository.getInstance(CSRDatabase.getInstance(context.applicationContext).jobDao())
        return JobSelectionViewModelFactory(repository)
    }

    fun provideJobDetailViewModelFactory(context: Context): JobDetailViewModelFactory {
        val repository =
            JobRepository.getInstance(CSRDatabase.getInstance(context.applicationContext).jobDao())
        return JobDetailViewModelFactory(repository)
    }
}