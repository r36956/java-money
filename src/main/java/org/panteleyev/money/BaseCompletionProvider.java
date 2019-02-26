/*
 * Copyright (c) 2019, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.panteleyev.money;

import javafx.util.Callback;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseCompletionProvider<T> implements Callback<AutoCompletionBinding.ISuggestionRequest,
    Collection<T>>
{
    private final Set<T> set;

    public BaseCompletionProvider(Set<T> set) {
        this.set = set;
    }

    public abstract String getElementString(T element);

    @Override
    public Collection<T> call(AutoCompletionBinding.ISuggestionRequest req) {
        if (req.getUserText().length() >= Options.getAutoCompleteLength()) {
            var userText = req.getUserText();

            var result = set.stream()
                .filter(it -> getElementString(it).toLowerCase().contains(userText.toLowerCase()))
                .collect(Collectors.toList());

            if (result.size() == 1 && getElementString(result.get(0)).equals(userText)) {
                /* If there is a single case sensitive match then no suggestions must be shown. */
                return List.of();
            } else {
                return result;
            }
        } else {
            return List.of();
        }
    }
}