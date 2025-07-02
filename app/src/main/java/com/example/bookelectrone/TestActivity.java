package com.example.bookelectrone;

import static com.example.bookelectrone.HelpClass.StyleTemplates.createAlphaAnimation;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.example.bookelectrone.HelpClass.LocaleHelper;
import com.example.bookelectrone.HelpClass.StyleTemplates;
import com.example.bookelectrone.db.BookDBManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestActivity extends AppCompatActivity {

    Context context;
    public BookDBManager dbManager;
    private BookDBManager.Test TEST;
    private int PROGRESS = 0;

    boolean MODE = false;

    public static class StudentAnswer {
        public int qi;
        public List<Integer> si;

        public StudentAnswer() {
            si = new ArrayList<>();
        }

        public StudentAnswer setAnswer(int q, List<BookDBManager.Test.Question.Answer> i) {
            qi = q;
            si.clear();
            for (BookDBManager.Test.Question.Answer a : i) {
                si.add(a.order);
            }
            return this;
        }
    }
    private List<StudentAnswer> StAnswers;

    LinearLayout linearLayout;
    ScrollView questionLayout;
    LinearLayout reviewContainer;

    ProgressBar progressBar;
    TextView progressText;

    BookDBManager.Test.Question currentQuestion;

    @Override protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String selectedLan = prefs.getString("selected_lan", "English");
        Context context = LocaleHelper.updateLocale(newBase, selectedLan);
        super.attachBaseContext(context);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbManager = new BookDBManager(this);
        context = this;
        linearLayout = findViewById(R.id.answers_container);
        questionLayout = findViewById(R.id.question_layout);
        reviewContainer = findViewById(R.id.review_container);

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);

        StAnswers = new ArrayList<>();


        // delete margin
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @NonNull @Override public WindowInsetsCompat
            onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets)
            {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = insets.getSystemWindowInsetBottom();
                v.setLayoutParams(params);
                return insets.consumeSystemWindowInsets();
            }
        });


        // back in activity
        findViewById(R.id.backButton).setOnClickListener(v -> {
                if (!TEST.STATE) new AlertDialog.Builder(TestActivity.this)
                    .setTitle(R.string.warning_label)
                    .setMessage(R.string.warning_test_label)
                    .setPositiveButton(R.string.ok_label, (d, z) -> agoToHome())
                    .setNegativeButton(R.string.cancel_label, null)
                    .show();
                else if (MODE) agoToHome();
                else setTestOnDB();
            }
        );
        findViewById(R.id.logout_button).setOnClickListener(v -> setTestOnDB());
        findViewById(R.id.logout_button_q).setOnClickListener(v -> agoToHome());


        // next button
        findViewById(R.id.next_button).setOnClickListener(v -> {
            if (!validateUserSelection()) return;
            if (currentQuestion.checkAnswer(getUserSelectedAnswers())) TEST.GRADE = TEST.GRADE + 1;
            PROGRESS = PROGRESS + 1;

            StAnswers.add(new StudentAnswer().setAnswer(currentQuestion.id, getUserSelectedAnswers()));

            questionLayout.startAnimation(createAlphaAnimation(1f, 0f, () -> {
                if (PROGRESS < TEST.questions.size()) showCurrentQuestion();
                else showInterfaceTest(true);
            }, 150));
        });


        StyleTemplates.appColorTheme(this);
        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }

    private void setTestOnDB() {
        dbManager.updateTest(TEST._ID, null,
                TEST.STATE, TEST.GRADE, new Gson().toJson(StAnswers),
                (q) -> agoToHome());
    }
    private void agoToHome() {
        Intent intent = new Intent(TestActivity.this, MainActivity.class);
        intent.putExtra("EXTRA_BTN", "Tests");
        startActivity(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        dbManager.openDB();

        Intent intent = getIntent();
        int id = intent.getIntExtra("EXTRA_ID", 0);
        BookDBManager.Test t = dbManager.getTest(id);

        loadTestInFile(t);

        if (t.STATE) {
            showQuestionsAnswers();
            MODE = true;
        }
        else {
            showCurrentQuestion();
            showInterfaceTest(false);
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }



    private void loadTestInFile(BookDBManager.Test t) {
        File testsDir = new File(context.getFilesDir(), "tests");
        File jsonFile = new File(testsDir, t.PATH);
        StringBuilder jsonBuilder = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(jsonFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        jsonBuilder.append(line);
        } catch (IOException e) { showErrorLabel(); }

        String jsonString = jsonBuilder.toString();
        Gson gson = new Gson();
        TEST = gson.fromJson(jsonString, BookDBManager.Test.class);
        TEST.setTest(t._ID, t.TITLE, t.STATE, t.GRADE, t.QUESTION_ANSWERS, t.USER_LOGIN);

        if (TEST.questions != null) {
            int questionCount = 0;
            for (BookDBManager.Test.Question question : TEST.questions) {
                question.id = questionCount++;
                int orderCounter = 0;
                for (BookDBManager.Test.Question.Answer answer : question.answers) {
                    answer.order = orderCounter++;
                }
            }

            if (!TEST.STATE) {
                Collections.shuffle(TEST.questions);

                for (int i = 0; i < TEST.questions.size(); i++) {
                    TEST.questions.get(i).displayOrder = i;
                    if (TEST.questions.get(i).answers != null) {
                        Collections.shuffle(TEST.questions.get(i).answers);
                        int ansIndex = 0;
                        for (BookDBManager.Test.Question.Answer answer : TEST.questions.get(i).answers) {
                            answer.displayOrder = ansIndex++;
                        }
                    }
                }
            }
        }

        ((TextView) findViewById(R.id.name_test_label)).setText(TEST.TITLE);
    }

    @NonNull private LinearLayout.LayoutParams getParamsTest() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16,4,16,4);
        return params;
    }
    @SuppressLint("SetTextI18n")
    public void showCurrentQuestion() {
        currentQuestion = TEST.questions.get(PROGRESS);

        TextView questionTextView = findViewById(R.id.question_text);
        questionTextView.setText(currentQuestion.text);
        linearLayout.removeAllViews();

        int totalQuestions = TEST.questions.size();
        progressBar.setMax(totalQuestions);
        progressBar.setProgress(PROGRESS);
        progressText.setText(getString(R.string.question_label) + " " +
                (PROGRESS + 1) + getString(R.string.from_label) + totalQuestions);

        if ("radio".equals(currentQuestion.type)) {
            RadioGroup radioGroup = new RadioGroup(this);
            for (BookDBManager.Test.Question.Answer answer : currentQuestion.answers) {
                RadioButton rb = new RadioButton(this);
                rb.setText(answer.text);
                rb.setTag(answer);
                rb.setLayoutParams(getParamsTest());
                radioGroup.addView(rb);
            }
            linearLayout.addView(radioGroup);
        }
        else if ("checkbox".equals(currentQuestion.type)) {
            for (BookDBManager.Test.Question.Answer answer : currentQuestion.answers) {
                CheckBox cb = new CheckBox(this);
                cb.setText(answer.text);
                cb.setTag(answer);
                cb.setLayoutParams(getParamsTest());
                linearLayout.addView(cb);
            }
        }

        StyleTemplates.applyFontToAllTextViews(linearLayout, this);
        questionLayout.startAnimation(createAlphaAnimation(0f, 1f, null, 150));
    }

    @NonNull private List<BookDBManager.Test.Question.Answer> getUserSelectedAnswers() {
        List<BookDBManager.Test.Question.Answer> userAnswers = new ArrayList<>();

        if ("radio".equals(currentQuestion.type)) {
            RadioGroup group = (RadioGroup) linearLayout.getChildAt(0);
            if (group != null) {
                int selectedId = group.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton rb = group.findViewById(selectedId);
                    if (rb != null && rb.getTag() instanceof BookDBManager.Test.Question.Answer)
                        userAnswers.add((BookDBManager.Test.Question.Answer) rb.getTag());
                }
            }
        } else if ("checkbox".equals(currentQuestion.type)) {
            int count = linearLayout.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = linearLayout.getChildAt(i);
                if (v instanceof CheckBox) {
                    CheckBox cb = (CheckBox) v;
                    if (cb.isChecked() && cb.getTag() instanceof BookDBManager.Test.Question.Answer)
                        userAnswers.add((BookDBManager.Test.Question.Answer) cb.getTag());
                }
            }
        }
        return userAnswers;
    }
    private boolean validateUserSelection() {
        List<BookDBManager.Test.Question.Answer> selected = getUserSelectedAnswers();
        if (selected == null || selected.isEmpty()) {
            Toast.makeText(this, getString(R.string.select_answer_label), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private void showInterfaceTest(boolean isEnd) {
        TEST.STATE = isEnd;

        // выключаем экран теста
        questionLayout.setVisibility(!isEnd ? View.VISIBLE : View.GONE);
        findViewById(R.id.next_button).setEnabled(!isEnd);
        findViewById(R.id.next_button).setClickable(!isEnd);

        // включаем экран окончания
        findViewById(R.id.result_layout).setVisibility(isEnd ? View.VISIBLE : View.GONE);
        findViewById(R.id.logout_button).setEnabled(isEnd);
        findViewById(R.id.logout_button).setClickable(isEnd);

        if (isEnd) {
            findViewById(R.id.result_layout).startAnimation(createAlphaAnimation(0f, 1f, () ->
                    findViewById(R.id.result_layout).setVisibility(View.VISIBLE), 150));

            linearLayout.removeAllViews();
            ((TextView) findViewById(R.id.true_results)).setHint(
                    String.valueOf(TEST.GRADE) + getString(R.string.from_label) + String.valueOf(TEST.questions.size())
            );
            TEST.GRADE = (TEST.GRADE * 10) / TEST.questions.size();
            ((TextView) findViewById(R.id.grade_label)).setHint(String.valueOf(TEST.GRADE));
        }
    }
    private void showErrorLabel() {
        questionLayout.setVisibility(View.GONE);
        findViewById(R.id.next_button).setEnabled(false);
        findViewById(R.id.next_button).setClickable(false);
        linearLayout.removeAllViews();

        TextView textView = new TextView(this, null, 0, R.style.LABEL_EMPTY);
        textView.setText(getString(R.string.error_load_test_label));

        questionLayout.removeAllViews();
        questionLayout.addView(textView);
    }



    @SuppressLint("SetTextI18n")
    private void showQuestionsAnswers() {
        List<StudentAnswer> restoredList = new Gson().fromJson(TEST.QUESTION_ANSWERS,
                new TypeToken<List<StudentAnswer>>() {}.getType());

        View questionAnswersLayout = findViewById(R.id.question_answers_layout);
        questionAnswersLayout.setVisibility(View.VISIBLE);
        questionAnswersLayout.setEnabled(true);
        questionAnswersLayout.setClickable(true);

        reviewContainer.removeAllViews();

        int correctCount = 0;

        for (int qIndex = 0; qIndex < TEST.questions.size(); qIndex++) {
            BookDBManager.Test.Question question = TEST.questions.get(qIndex);

            StudentAnswer studentAnswer = null;
            if (restoredList != null) {
                for (StudentAnswer sa : restoredList) {
                    if (sa.qi == question.id) {
                        studentAnswer = sa; break;
                    }
                }
            }

            LinearLayout questionContainer = new LinearLayout(this);
            questionContainer.setOrientation(LinearLayout.VERTICAL);
            questionContainer.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerParams.setMargins(0, 0, 0, 16);
            questionContainer.setLayoutParams(containerParams);

            TextView questionTitleView = new TextView(this);
            questionTitleView.setText(getString(R.string.question_label) + " " + (qIndex + 1) + ": " + question.text);
            questionTitleView.setTextSize(18);
            questionContainer.addView(questionTitleView);

            LinearLayout answersLayout = new LinearLayout(this);
            answersLayout.setOrientation(LinearLayout.VERTICAL);
            answersLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            answersLayout.setPadding(0, 8, 0, 8);

            List<BookDBManager.Test.Question.Answer> studentSelectedAnswers = new ArrayList<>();

            if ("radio".equals(question.type)) {
                RadioGroup radioGroup = new RadioGroup(this);
                radioGroup.setOrientation(RadioGroup.VERTICAL);
                for (BookDBManager.Test.Question.Answer answer : question.answers) {
                    RadioButton rb = new RadioButton(this);
                    rb.setText(answer.text);
                    rb.setLayoutParams(getParamsTest());
                    rb.setEnabled(false);

                    boolean isSelected = (studentAnswer != null && studentAnswer.si.contains(answer.order));
                    if (isSelected) { rb.setChecked(true); studentSelectedAnswers.add(answer); }
                    if (answer.isCorrect) rb.setTextColor(BookDBManager.Bookmark.GREEN_COLOR);
                    else if (isSelected && !answer.isCorrect) rb.setTextColor(BookDBManager.Bookmark.RED_COLOR);
                    radioGroup.addView(rb);
                }
                answersLayout.addView(radioGroup);
            } else if ("checkbox".equals(question.type)) {
                for (BookDBManager.Test.Question.Answer answer : question.answers) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(answer.text);
                    cb.setLayoutParams(getParamsTest());
                    cb.setEnabled(false);

                    boolean isSelected = (studentAnswer != null && studentAnswer.si.contains(answer.order));
                    if (isSelected) { cb.setChecked(true); studentSelectedAnswers.add(answer); }
                    if (answer.isCorrect) cb.setTextColor(BookDBManager.Bookmark.GREEN_COLOR);
                    else if (isSelected && !answer.isCorrect) cb.setTextColor(BookDBManager.Bookmark.RED_COLOR);
                    answersLayout.addView(cb);
                }
            }
            questionContainer.addView(answersLayout);

            boolean isCorrect = question.checkAnswer(studentSelectedAnswers);
            if (isCorrect) correctCount++;

            TextView questionResult = new TextView(this);
            questionResult.setText(isCorrect ? getString(R.string.answer_true_label) : getString(R.string.answer_false_label));
            questionResult.setTextSize(16);
            questionResult.setTextColor(isCorrect ? BookDBManager.Bookmark.GREEN_COLOR : BookDBManager.Bookmark.RED_COLOR);
            questionContainer.addView(questionResult);

            reviewContainer.addView(questionContainer);
        }

        ((TextView) findViewById(R.id.true_results_q))
                .setHint(correctCount + getString(R.string.from_label) + TEST.questions.size());
        ((TextView) findViewById(R.id.grade_label_q))
                .setHint(String.valueOf(TEST.GRADE));

        StyleTemplates.applyFontToAllTextViews((ViewGroup) findViewById(R.id.main), this);
    }




}