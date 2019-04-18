/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
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
package de.mirkosertic.desktopsearch;

import java.util.Locale;

public enum SupportedLanguage {
    ar,
    bg,
    br,
    ca,
    ckb,
    cz,
    da,
    de,
    el,
    en,
    es,
    eu,
    fa,
    fi,
    fr,
    ga,
    gl,
    hi,
    hu,
    hy,
    id,
    it,
    lv,
    nl,
    no,
    pt,
    ro,
    ru,
    sv,
    th,
    tr,;

    public static SupportedLanguage getDefault() {
        try {
            return SupportedLanguage.valueOf(Locale.getDefault().getLanguage());
        } catch (final Exception e) {
            return SupportedLanguage.en;
        }
    }

    public Locale toLocale() {
        return new Locale(name());
    }
}