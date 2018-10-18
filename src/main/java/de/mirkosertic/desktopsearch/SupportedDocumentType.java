/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
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
