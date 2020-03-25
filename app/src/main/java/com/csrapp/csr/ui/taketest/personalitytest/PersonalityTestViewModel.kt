package com.csrapp.csr.ui.taketest.personalitytest

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.csrapp.csr.data.PersonalityQuestionEntity
import com.csrapp.csr.data.PersonalityQuestionRepository
import kotlin.collections.set

class PersonalityTestViewModel(private val personalityQuestionRepository: PersonalityQuestionRepository) :
    ViewModel() {

    private val TAG = "PersonalityTestVM"

    // Sentiment analysis questions skipped due to internet problems.
    private val sentimentalQuestionsSkipped = mutableMapOf<String, Int>()

    private var _nluErrorOccurred = MutableLiveData<NLUError?>(null)
    val nluErrorOccurred: LiveData<NLUError?>
        get() = _nluErrorOccurred

    enum class NLUError {
        INTERNET, BAD_RESPONSE
    }

    val questionsPerStream = 2

    private var _currentQuestionIndex = MutableLiveData(0)
    val currentQuestionIndex: LiveData<Int>
        get() = _currentQuestionIndex

    private var _currentQuestionNumberDisplay = MutableLiveData<String>()
    val currentQuestionNumberDisplay: LiveData<String>
        get() = _currentQuestionNumberDisplay

    private var _testFinished = MutableLiveData(false)
    val testFinished: LiveData<Boolean>
        get() = _testFinished

    private var _btnNextText = MutableLiveData<String>("Next")
    val btnNextText: LiveData<String>
        get() = _btnNextText

    private var _sliderValueText = MutableLiveData<String>()
    val sliderValueText: LiveData<String>
        get() = _sliderValueText

    var sliderValue = MutableLiveData<Int>()

    private var _currentQuestion = MutableLiveData<PersonalityQuestionEntity>()
    val currentQuestion: LiveData<PersonalityQuestionEntity>
        get() = _currentQuestion

    var responseString = MutableLiveData<String>()

    private var _isTextualQuestion = MutableLiveData<Boolean>()
    val isTextualQuestion: LiveData<Boolean>
        get() = _isTextualQuestion

    private var questionsAndResponses: List<PersonalityQuestionAndResponseHolder>

    private var sliderValueObserver: Observer<Int>

    init {
        val questions = getRandomizedQuestions()
        val tempQuestionHolders = mutableListOf<PersonalityQuestionAndResponseHolder>()
        for (i in questions.indices) {
            tempQuestionHolders.add(PersonalityQuestionAndResponseHolder(questions[i]))
        }

        for (stream in getStreams()) {
            sentimentalQuestionsSkipped[stream] = 0
        }
        Log.d(TAG, "Questions per stream = $questionsPerStream")

        questionsAndResponses = tempQuestionHolders
        _currentQuestion.value = questionsAndResponses[_currentQuestionIndex.value!!].question
        _isTextualQuestion.value = _currentQuestion.value!!.type == "textual"

        sliderValue.value = 0
        sliderValueObserver = Observer { value ->
            _sliderValueText.value = "${value}%"
        }
        sliderValue.observeForever(sliderValueObserver)
        _currentQuestionNumberDisplay.value = generateCurrentQuestionNumber()
    }

    private fun generateCurrentQuestionNumber(): String {
        val currentQuestionNumber = _currentQuestionIndex.value!! + 1
        val totalQuestions = questionsAndResponses.size

        return "Question $currentQuestionNumber/$totalQuestions"
    }

    fun performSentimentAnalysis(string: String): Double? {
        // TODO: Perform actual sentiment analysis.
        val score: Double? = null

        if (score == null) {
            _nluErrorOccurred.value = NLUError.BAD_RESPONSE
        }

        return score
    }

    fun skipCurrentQuestion() {
        val stream = currentQuestion.value!!.stream!!
        val previousSkipped = sentimentalQuestionsSkipped[stream]!!
        sentimentalQuestionsSkipped[stream] = previousSkipped + 1

        questionsAndResponses[_currentQuestionIndex.value!!].score = 0.0

        if (_currentQuestionIndex.value == questionsAndResponses.lastIndex) {
            _testFinished.value = true
            return
        } else if (_currentQuestionIndex.value == questionsAndResponses.lastIndex - 1) {
            _btnNextText.value = "Finish"
        }

        _currentQuestionIndex.value = _currentQuestionIndex.value!! + 1
        updateUI()
    }

    fun onButtonNextClicked() {
        val score: Double?

        when (currentQuestion.value!!.type) {
            "textual" -> {
                score = performSentimentAnalysis(responseString.value!!)
                // Allow user to retry or skip the question on encountering an error
                // while performing sentiment analysis.
                if (score == null) {
                    return
                }
            }
            else -> {
                score = sliderValue.value!!.toDouble()
            }
        }

        questionsAndResponses[_currentQuestionIndex.value!!].score = score

        if (_currentQuestionIndex.value == questionsAndResponses.lastIndex) {
            _testFinished.value = true
            return
        } else if (_currentQuestionIndex.value == questionsAndResponses.lastIndex - 1) {
            _btnNextText.value = "Finish"
        }

        _currentQuestionIndex.value = _currentQuestionIndex.value!! + 1
        updateUI()
    }

    private fun updateUI() {
        _currentQuestionNumberDisplay.value = generateCurrentQuestionNumber()

        _currentQuestion.value = questionsAndResponses[_currentQuestionIndex.value!!].question
        _isTextualQuestion.value = _currentQuestion.value!!.type == "textual"

        sliderValue.value = 0
        responseString.value = ""
    }

    private fun getRandomizedQuestions(): List<PersonalityQuestionEntity> {
        val questions = mutableListOf<PersonalityQuestionEntity>()
        val streams = personalityQuestionRepository.getStreams()
        streams.forEach { stream ->
            questions.addAll(
                personalityQuestionRepository.getQuestionsByStream(
                    stream,
                    questionsPerStream
                )
            )
        }
        return questions
    }

    fun getQuestionsAndResponses() = questionsAndResponses

    fun getStreams() = personalityQuestionRepository.getStreams()

    fun getQuestionsSkippedInEachStream() = sentimentalQuestionsSkipped

    override fun onCleared() {
        sliderValue.removeObserver(sliderValueObserver)
        super.onCleared()
    }
}