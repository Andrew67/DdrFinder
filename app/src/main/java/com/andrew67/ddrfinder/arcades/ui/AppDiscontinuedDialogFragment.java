/*
 * Copyright (c) 2018-2025 Andrés Cordero
 * Web: https://github.com/Andrew67/DdrFinder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.andrew67.ddrfinder.arcades.ui;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.andrew67.ddrfinder.R;

public class AppDiscontinuedDialogFragment extends DialogFragment {

    private OnOkClickListener okClickListener = null;

    public AppDiscontinuedDialogFragment setOkListener(OnOkClickListener okClickListener) {
        this.okClickListener = okClickListener;
        return this;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // From https://developer.android.com/guide/topics/ui/dialogs.html#DialogFragment
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        builder.setMessage(R.string.eol_text)
                .setTitle(R.string.eol_title)
                .setPositiveButton(R.string.eol_ok, (dialog, id) -> this.okClickListener.onOkClick())
                .setCancelable(false);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface OnOkClickListener {
        void onOkClick();
    }
}
