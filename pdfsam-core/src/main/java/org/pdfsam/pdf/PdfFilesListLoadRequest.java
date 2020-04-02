/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 30 apr 2019
 * Copyright 2017 by Sober Lemur S.a.s di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.pdf;

import static java.util.Objects.nonNull;
import static org.pdfsam.support.RequireUtils.requireNotBlank;
import static org.pdfsam.support.RequireUtils.requireNotNull;
import static org.sejda.eventstudio.StaticStudio.eventStudio;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.module.ModuleOwned;
import org.sejda.eventstudio.annotation.EventListener;

/**
 * @author Andrea Vacondio
 *
 */
public class PdfFilesListLoadRequest implements ModuleOwned {
    public final Path list;
    private String ownerModule = StringUtils.EMPTY;

    public PdfFilesListLoadRequest(String ownerModule, Path list) {
        requireNotBlank(ownerModule, "Owner module cannot be blank");
        requireNotNull(list, "List file cannot be null");
        this.ownerModule = ownerModule;
        this.list = list;
    }

    @Override
    public String getOwnerModule() {
        return ownerModule;
    }

    /**
     * Request to load a text/csv file containing a list of PDF
     *
     * @param pdfLoadController
     */
    @EventListener
    public void request(PdfLoadController pdfLoadController) {
        PdfLoadController.LOG.trace("PDF load from list request received");
        if (nonNull(list)) {
            pdfLoadController.executor.execute(() -> {
                try {
                    PdfLoadRequestEvent loadEvent = new PdfLoadRequestEvent(getOwnerModule());
                    new PdfListParser().apply(list).stream().map(PdfDocumentDescriptor::newDescriptorNoPassword)
                            .forEach(loadEvent::add);
                    if (loadEvent.getDocuments().isEmpty()) {
                        PdfLoadController.LOG.error(DefaultI18nContext.getInstance()
                                .i18n("Unable to find any valid PDF file in the list: {0}", list.toString()));
                    } else {
                        eventStudio().broadcast(loadEvent, getOwnerModule());
                    }
                } catch (Exception e) {
                    PdfLoadController.LOG.error(DefaultI18nContext.getInstance().i18n("Unable to load PDF list file from {0}",
                            list.toString()), e);
                }
            });
        }
    }
}
