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

import java.io.File;
import java.util.Locale;

public enum SupportedDocumentType {
    txt {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Text files (.txt)";
        }
    },
    msg {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft Outlook Messages (.msg)";
        }
    },
    pdf {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Adobe PDF Files (.pdf)";
        }
    },
    doc {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft Word Documents (.doc)";
        }
    },
    docx {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft Word Documents (.docx)";
        }
    },
    ppt {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft PowerPoint Documents (.ppt)";
        }
    },
    pptx {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft PowerPoint Documents (.pptx)";
        }
    },
    rtf {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Rich Text File (.rtf)";
        }
    },
    html {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "HTML Document (.html)";
        }
    },
    htm {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "HTML Document (.htm)";
        }
    },
    eml {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Microsoft Outlook Messages (.eml)";
        }
    },
    ini {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Configuration Files (.ini)";
        }
    },
    epub {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "EBook (.epub)";
        }
    },
    mobi {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "EBook (.mobi)";
        }
    },
    odt {
        @Override
        public String getDisplayName(final Locale aLocale) {
            return "Open Office Document (.odt)";
        }
    };

    public boolean supports(final String aFilename) {
        // Filter by extension and also make sure no temp files are indexed...
        return aFilename.toLowerCase().endsWith("." + name()) && !aFilename.contains("~");
    }

    public abstract String getDisplayName(Locale aLocale);

    public boolean matches(final File aFile) {
        return aFile.getName().toLowerCase().endsWith("." +name());
    }
}
