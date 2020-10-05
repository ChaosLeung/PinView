/*
 * Copyright 2017 Chaos Leong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chaos.view.example;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.chaos.view.PinView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author Chaos Leong
 *         07/04/2017
 */

public class SecondActivity extends AppCompatActivity implements CheckBox.OnCheckedChangeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        ((PinView) findViewById(R.id.firstPinView)).setAnimationEnable(true);
        ((PinView) findViewById(R.id.secondPinView)).setAnimationEnable(true);
        ((CheckBox) findViewById(R.id.firstPasswordHidden)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.secondPasswordHidden)).setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (compoundButton.getId() == R.id.firstPasswordHidden)
            ((PinView) findViewById(R.id.firstPinView)).setPasswordHidden(isChecked);
        else
            ((PinView) findViewById(R.id.secondPinView)).setPasswordHidden(isChecked);
    }
}
