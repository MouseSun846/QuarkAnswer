package com.example.quarkanswer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.shixia.colorpickerview.ColorPickerView;
import com.shixia.colorpickerview.OnColorChangeListener;

public class SetColor extends AppCompatActivity {
    private TextView tvTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_color);
        tvTest = findViewById(R.id.tv_test);
        SharedPreferences sp = getApplicationContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sp.edit();
        ColorPickerView colorPicker = findViewById(R.id.cpv_color_picker);
        colorPicker.setOnColorChangeListener(new OnColorChangeListener() {
            @Override
            public void colorChanged(int color) {
                editor.putInt("color",color);
                editor.commit();
                tvTest.setBackgroundColor(color);
            }
        });

    }
}
