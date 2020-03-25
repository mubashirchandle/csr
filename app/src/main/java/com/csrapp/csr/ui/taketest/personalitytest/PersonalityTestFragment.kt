package com.csrapp.csr.ui.taketest.personalitytest

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.csrapp.csr.R
import com.csrapp.csr.databinding.FragmentPersonalityTestBinding
import com.csrapp.csr.utils.InjectorUtils
import kotlin.math.roundToInt

class PersonalityTestFragment : Fragment() {

    private val TAG = "PersonalityTestFragment"

    private lateinit var navController: NavController
    private lateinit var viewModel: PersonalityTestViewModel
    private lateinit var binding: FragmentPersonalityTestBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backConfirmationDialog = AlertDialog.Builder(requireContext())
            .setTitle("Quit Test")
            .setMessage("Are you sure you want to quit the test?")
            .setPositiveButton("Yes") { _, _ ->
                navController.navigateUp()
            }
            .setNegativeButton("Cancel") { _, _ ->
                println("Cancel pressed")
            }
            .create()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            backConfirmationDialog.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_personality_test, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        val factory = InjectorUtils.providePersonalityTestViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory).get(PersonalityTestViewModel::class.java)

        sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            MODE_PRIVATE
        )

        val isAptitudeTestCompleted = sharedPreferences.getBoolean(
            getString(R.string.shared_preferences_aptitude_test_completed),
            false
        )
        if (!isAptitudeTestCompleted) {
            throw Exception("Personality Test started without completing Aptitude Test")
        }

        binding.personalityViewModel = viewModel
        binding.lifecycleOwner = this

        val testFinishObserver = Observer<Boolean> { testFinished ->
            if (testFinished) {
                generateResult()

                val testCompletionDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Personality Test Completed")
                    .setMessage("You have successfully completed the second step of the test.\n\nYou can now view your result.")
                    .setPositiveButton("Okay", null)
                    .create()
                testCompletionDialog.show()
                navController.navigateUp()
            }
        }
        viewModel.testFinished.observe(viewLifecycleOwner, testFinishObserver)

        viewModel.nluErrorOccurred.observe(viewLifecycleOwner) { errorOccurred ->
            when (errorOccurred) {
                PersonalityTestViewModel.NLUError.INTERNET -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Error while connecting to the internet. Please check your internet connection.")
                        .setPositiveButton("Try Again") { _, _ ->
                            viewModel.performSentimentAnalysis(viewModel.responseString.toString())
                        }
                        .setNegativeButton("Skip This Question") { _, _ ->
                            viewModel.skipCurrentQuestion()
                        }
                        .create()
                        .show()
                }
                PersonalityTestViewModel.NLUError.BAD_RESPONSE -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Unable to Analyze")
                        .setMessage("Please check to make sure you have not made any spelling mistakes.")
                        .setPositiveButton("Okay", null)
                        .setNegativeButton("Skip This Question") { _, _ ->
                            viewModel.skipCurrentQuestion()
                        }
                        .create()
                        .show()
                }
            }

        }
    }

    private fun generateResult(): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        viewModel.getStreams().forEach { stream ->
            result[stream] = 0.0
        }

        val questionsSkippedInEachStream = viewModel.getQuestionsSkippedInEachStream()

        val questionsAndResponses = viewModel.getQuestionsAndResponses()
        questionsAndResponses.forEach { questionAndResponse ->
            val question = questionAndResponse.question
            val questionsAnswered =
                viewModel.questionsPerStream - questionsSkippedInEachStream[question.stream]!!
            var currentScore = (questionAndResponse.score!!).toDouble() / questionsAnswered

            // Score can be NaN if all the questions of a stream are skipped.
            if (currentScore.isNaN())
                currentScore = 0.0

            val previousScore = result[question.stream]!!
            result[question.stream!!] = previousScore + currentScore
        }

        Log.d(TAG, result.toString())

        with(sharedPreferences.edit()) {
            putBoolean(getString(R.string.shared_preferences_personality_test_completed), true)
            result.forEach { (stream, score) ->
                putInt(stream, score.roundToInt())
                Log.d(TAG, "$stream: ${score.roundToInt()}")
            }
            commit()
        }

        return result
    }
}