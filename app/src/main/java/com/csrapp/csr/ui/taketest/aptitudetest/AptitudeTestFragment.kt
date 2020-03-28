package com.csrapp.csr.ui.taketest.aptitudetest

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.csrapp.csr.R
import com.csrapp.csr.utils.InjectorUtils
import com.csrapp.csr.utils.ResourceProvider
import kotlinx.android.synthetic.main.fragment_aptitude_test.*
import kotlin.math.roundToInt

class AptitudeTestFragment : Fragment(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener,
    AdapterView.OnItemSelectedListener {

    private lateinit var navController: NavController
    private lateinit var viewModel: AptitudeTestViewModel
    private lateinit var spinnerAdapter: SpinnerQuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backConfirmationDialog = AlertDialog.Builder(requireContext())
            .setTitle(ResourceProvider.getString(R.string.quit_test))
            .setMessage(ResourceProvider.getString(R.string.aptitude_test_quit_confirmation))
            .setPositiveButton(ResourceProvider.getString(R.string.yes)) { _, _ ->
                navController.navigateUp()
            }
            .setNegativeButton(ResourceProvider.getString(R.string.no), null)
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
        return inflater.inflate(R.layout.fragment_aptitude_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        val factory = InjectorUtils.provideAptitudeTestViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory).get(AptitudeTestViewModel::class.java)

        spinnerAdapter = viewModel.getSpinnerAdapter(requireContext())
        spinnerQuestions.adapter = spinnerAdapter

        viewModel.timeRemaining.observe(this) {
            val minutes = it / 60
            val seconds = it % 60

            remainingTime.text =
                ResourceProvider.getString(R.string.remaining_time, minutes, seconds)

            if (it == 0L) {
                finishTest(finishedByTimer = true)
            }
        }

        assignActionListeners()
        updateButtons()
    }

    private fun assignActionListeners() {
        btnNext.setOnClickListener(this)
        btnMark.setOnClickListener(this)
        btnClear.setOnClickListener(this)

        optionGroup.setOnCheckedChangeListener(this)

        spinnerQuestions.onItemSelectedListener = this

        confidenceSeekBar.setOnSeekBarChangeListener(this)
    }

    private fun updateUI() {
        questionScrollView.smoothScrollTo(0, 0)
        spinnerQuestions.setSelection(viewModel.currentQuestionIndex)

        val questionAndResponseHolder = spinnerAdapter.getItem(viewModel.currentQuestionIndex)!!
        val question = questionAndResponseHolder.question
        questionText.text = question.text

        if (question.referenceImage.isNullOrBlank()) {
            referenceImage.visibility = View.GONE
        } else {
            referenceImage.visibility = View.VISIBLE
            val identifier = requireContext().resources.getIdentifier(
                question.referenceImage,
                "mipmap",
                requireContext().packageName
            )
            referenceImage.setImageResource(identifier)
        }

        option1.text = question.option1
        option2.text = question.option2
        option3.text = question.option3
        option4.text = question.option4

        // Check if this question was answered previously.
        if (questionAndResponseHolder.optionSelected == null) {
            optionGroup.clearCheck()
        } else {
            updateConfidenceTextView()
            when (questionAndResponseHolder.optionSelected) {
                1 -> optionGroup.check(R.id.option1)
                2 -> optionGroup.check(R.id.option2)
                3 -> optionGroup.check(R.id.option3)
                4 -> optionGroup.check(R.id.option4)
            }
        }

        if (questionAndResponseHolder.confidence == null)
            confidenceSeekBar.progress = 0
        else
            confidenceSeekBar.progress = questionAndResponseHolder.confidence!! - 1

        updateConfidenceTextView()
        updateButtons()
    }

    private fun updateButtons() {
        if (optionGroup.checkedRadioButtonId == -1) {
            confidenceSeekBar.isEnabled = false
            btnMark.isEnabled = false
            btnClear.isEnabled = false
        } else {
            confidenceSeekBar.isEnabled = true
            updateConfidenceTextView()
            btnMark.isEnabled = true
            btnClear.isEnabled = true
        }

        if (viewModel.currentQuestionIndex == spinnerAdapter.count - 1) {
            btnNext.text = ResourceProvider.getString(R.string.finish)
            btnMark.text = ResourceProvider.getString(R.string.mark)
        } else {
            btnNext.text = ResourceProvider.getString(R.string.next)
            btnMark.text = ResourceProvider.getString(R.string.mark_and_next)
        }
    }

    private fun finishTest(finishedByTimer: Boolean) {
        btnNext.isEnabled = false
        btnMark.isEnabled = false
        btnClear.isEnabled = false

        saveScores()

        val message = when (finishedByTimer) {
            true -> ResourceProvider.getString(R.string.aptitude_test_completed_by_timer_msg)
            false -> ResourceProvider.getString(R.string.aptitude_test_completed_manually_msg)
        }

        val testCompletionDialog = AlertDialog.Builder(requireContext())
            .setTitle(ResourceProvider.getString(R.string.aptitude_test_completed))
            .setMessage(message)
            .setPositiveButton(ResourceProvider.getString(R.string.okay), null)
            .create()
        testCompletionDialog.show()
        navController.navigateUp()
    }

    private fun saveScores() {
        val scores = mutableMapOf<String, Double>()
        val questionsPerCategory = viewModel.questionsPerCategory

        for (i in 0 until spinnerAdapter.count) {
            val questionHolder = spinnerAdapter.getItem(i)!!
            if (questionHolder.responseType == AptitudeQuestionAndResponseHolder.QuestionResponseType.UNANSWERED)
                continue

            val questionScore =
                if (questionHolder.question.correctOption == questionHolder.optionSelected) {
                    // Convert to double to avoid integer division.
                    (questionHolder.confidence!!).toDouble() / questionsPerCategory
                } else {
                    (-questionHolder.confidence!!).toDouble() / questionsPerCategory
                }

            val previousScore = scores[questionHolder.question.category] ?: 0.0
            scores[questionHolder.question.category] = previousScore + questionScore
        }

        val sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            MODE_PRIVATE
        )
        with(sharedPreferences.edit()) {
            putBoolean(getString(R.string.shared_preferences_aptitude_test_completed), true)
            scores.forEach { (category, score) ->
                putInt(category, score.roundToInt())
            }
            commit()
        }
    }

    // Next/Mark/Skip Button clicked.
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btnNext -> {
                if (getSelectedOption() != null) {
                    val questionHolder = spinnerAdapter.getItem(viewModel.currentQuestionIndex)!!
                    questionHolder.responseType =
                        AptitudeQuestionAndResponseHolder.QuestionResponseType.ANSWERED
                    questionHolder.optionSelected = getSelectedOption()
                    questionHolder.confidence = confidenceSeekBar.progress

                }

                // If not the last question.
                if (viewModel.currentQuestionIndex != spinnerAdapter.count - 1) {
                    viewModel.currentQuestionIndex += 1
                    updateUI()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle(ResourceProvider.getString(R.string.finish_test))
                        .setMessage(ResourceProvider.getString(R.string.aptitude_test_submission_confirmation))
                        .setPositiveButton(ResourceProvider.getString(R.string.yes)) { _, _ ->
                            finishTest(finishedByTimer = false)
                        }
                        .setNegativeButton(ResourceProvider.getString(R.string.no), null)
                        .create()
                        .show()
                }
            }

            R.id.btnMark -> {
                if (getSelectedOption() != null) {
                    val questionHolder = spinnerAdapter.getItem(viewModel.currentQuestionIndex)!!
                    questionHolder.responseType =
                        AptitudeQuestionAndResponseHolder.QuestionResponseType.MARKED
                    questionHolder.optionSelected = getSelectedOption()
                    questionHolder.confidence = confidenceSeekBar.progress
                }

                // If not the last question.
                if (viewModel.currentQuestionIndex != spinnerAdapter.count - 1) {
                    viewModel.currentQuestionIndex += 1
                    updateUI()
                } else {
                    // Change the color of question number in spinner manually in the last question.
                    spinnerAdapter.notifyDataSetChanged()
                }
            }

            R.id.btnClear -> {
                val questionHolder = spinnerAdapter.getItem(viewModel.currentQuestionIndex)!!
                questionHolder.responseType =
                    AptitudeQuestionAndResponseHolder.QuestionResponseType.UNANSWERED
                questionHolder.optionSelected = null
                questionHolder.confidence = 0

                optionGroup.clearCheck()

                // Update to confidence TextView must be done after clearing option group.
                confidenceSeekBar.progress = 0
                updateConfidenceTextView()

                // Change the color of question number in spinner.
                spinnerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun getSelectedOption(): Int? {
        if (optionGroup.checkedRadioButtonId == -1)
            return null

        return when (optionGroup.checkedRadioButtonId) {
            R.id.option1 -> 1
            R.id.option2 -> 2
            R.id.option3 -> 3
            R.id.option4 -> 4
            else -> throw Exception("Selected option invalid")
        }
    }

    // Spinner item selected.
    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do Nothing.
    }

    // Spinner item selected.
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.currentQuestionIndex = position
        updateUI()
    }

    // Answer option selected.
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        updateButtons()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        updateConfidenceTextView()
    }

    private fun updateConfidenceTextView() {
        if (getSelectedOption() != null) {
            textViewConfidence.visibility = View.VISIBLE
            val percent = confidenceSeekBar.progress
            textViewConfidence.text = ResourceProvider.getString(R.string.percent, percent)
        } else {
            textViewConfidence.visibility = View.INVISIBLE
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // Do Nothing.
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // Do Nothing.
    }
}
