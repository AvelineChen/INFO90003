/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.wearmouse.ui.input;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.annotation.IntDef;
import com.ginkage.wearmouse.R;
import com.ginkage.wearmouse.bluetooth.HidDataSender;
import com.ginkage.wearmouse.input.KeyboardHelper;
import com.ginkage.wearmouse.input.KeyboardInputController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Implements a "card-flip" animation using custom fragment transactions. */
public class InputActivity extends WearableActivity {
    public static final String EXTRA_INPUT_MODE = "input_mode";
    private HidDataSender hidDataSender;
    private KeyboardHelper keyboardHelper;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({InputMode.MOUSE, InputMode.KEYPAD, InputMode.TOUCHPAD})
    public @interface InputMode {
        int MOUSE = 1;
        int KEYPAD = 2;
        int TOUCHPAD = 3;
    }

    private static final int INPUT_REQUEST_CODE = 1;

    private KeyboardInputController keyboardController;
    private @InputMode int currentMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_flip);
        setAmbientEnabled();

        keyboardController = new KeyboardInputController(this::finish);
        keyboardController.onCreate(this);

        this.hidDataSender = HidDataSender.getInstance();
        this.keyboardHelper = new KeyboardHelper(hidDataSender);

        currentMode = InputMode.MOUSE;
        Intent intent = getIntent();
        if (intent != null) {
            int mode = intent.getIntExtra(EXTRA_INPUT_MODE, -1);
            if (mode > 0) {
                currentMode = mode;
            }
        }

        Log.e("TEST", "RUNINING INPUT ACTIVITY");

        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, getFragment(currentMode))
                .commit();
    }
    //GABRIELE MARINI 969738
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardController.onResume();
    }

    @Override
    protected void onDestroy() {
        keyboardController.onDestroy(this);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            if (action == KeyEvent.ACTION_UP) {
                flipCard(
                        currentMode == InputMode.MOUSE
                                ? InputMode.TOUCHPAD
                                : currentMode == InputMode.TOUCHPAD
                                        ? InputMode.KEYPAD
                                        : InputMode.MOUSE);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
            if (action == KeyEvent.ACTION_UP) {
                Intent intent = keyboardController.getInputIntent(getPackageManager());
                if (intent != null) {
                    startActivityForResult(intent, INPUT_REQUEST_CODE);
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INPUT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            keyboardController.onActivityResult(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        return ((currentMode == InputMode.MOUSE
                                && ((MouseFragment) getFragment()).onGenericMotionEvent(ev))
                        || (currentMode == InputMode.TOUCHPAD
                                && ((TouchpadFragment) getFragment()).onGenericMotionEvent(ev)))
                || super.onGenericMotionEvent(ev);
    }

    private Fragment getFragment() {
        return getFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void flipCard(@InputMode int mode) {
        currentMode = mode;
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.card_flip_right_in, R.animator.card_flip_right_out)
                .replace(R.id.fragment_container, getFragment(currentMode))
                .commit();
    }

    private Fragment getFragment(@InputMode int mode) {
        return mode == InputMode.KEYPAD
                ? new KeypadFragment()
                : mode == InputMode.MOUSE ? new MouseFragment() : new TouchpadFragment();
    }

    @Override /* KeyEvent.Callback */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("TEST", "GOT A KEY DOWN");
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                // Do something that advances a user View to the next item in an ordered list.
                sendKeyPress(KeyboardHelper.Key.RIGHT);
                System.out.println("NEXT GESTURE");
                return true; //moveToNextItem();
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                // Do something that advances a user View to the previous item in an ordered list.
                sendKeyPress(KeyboardHelper.Key.LEFT);
                System.out.println("PREVIOUS GESTURE");
                return true; //moveToPreviousItem();
        }
        // If you did not handle it, let it be handled by the next possible element as deemed by the Activity.
        return super.onKeyDown(keyCode, event);
    }

    private void sendKeyPress(int key) {
        keyboardHelper.sendKeyDown(0, key);
        keyboardHelper.sendKeysUp(0);
    }

}
