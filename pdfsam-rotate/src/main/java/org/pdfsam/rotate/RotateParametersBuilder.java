/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 26/giu/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
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
package org.pdfsam.rotate;

import static java.util.Objects.isNull;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.pdfsam.support.params.AbstractPdfOutputParametersBuilder;
import org.pdfsam.support.params.MultipleOutputTaskParametersBuilder;
import org.pdfsam.task.BulkRotateParameters;
import org.pdfsam.task.PdfRotationInput;
import org.sejda.common.collection.NullSafeSet;
import org.sejda.model.input.PdfSource;
import org.sejda.model.output.SingleOrMultipleTaskOutput;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;

/**
 * Builder for {@link BulkRotateParameters}
 * 
 * @author Andrea Vacondio
 *
 */
class RotateParametersBuilder extends AbstractPdfOutputParametersBuilder<BulkRotateParameters>
        implements MultipleOutputTaskParametersBuilder<BulkRotateParameters> {

    private SingleOrMultipleTaskOutput output;
    private String prefix;
    private Set<PdfRotationInput> inputs = new NullSafeSet<>();
    private Rotation rotation;
    private PredefinedSetOfPages predefinedRotationType;

    void addInput(PdfSource<?> source, Set<PageRange> pageSelection, Integer totalPages) {
        if (isNull(pageSelection) || pageSelection.isEmpty()) {
            this.inputs.add(new PdfRotationInput(source, rotation, predefinedRotationType));
        } else {
            //invoking a new method to filter out even/odd pages for custom page ranges - change request ps3
            Set<PageRange> filteredPages = filterEvenOddPages(pageSelection, predefinedRotationType, totalPages);
            this.inputs.add(new PdfRotationInput(source, rotation, filteredPages.stream().toArray(PageRange[]::new)));
        }
    }

    protected Set<PageRange> filterEvenOddPages(Set<PageRange> pageSelection, PredefinedSetOfPages evenOddAll,
                                      Integer lastPage){
        //added  for ps3 to take a range of pages and remove the odd or even ones if EVEN_PAGES or ODD_PAGES selected
        if (evenOddAll.name().equals("ALL_PAGES")) {
            return pageSelection;
        }
        else {
            //only need to alter and flatten page ranges if EVEN or ODD pages are specified
            //This routine will alter 3-10 to 3,5,7,9 if ODD pages are selected
            return filteredPaged(pageSelection, evenOddAll, lastPage);
        }
    }

    private Set<PageRange> filteredPaged(Set<PageRange> pageSelection, PredefinedSetOfPages evenOddAll, Integer lastPage) {
        boolean even = evenOddAll.name().equals("EVEN_PAGES");
        Set<PageRange> filteredPages = new HashSet<PageRange>();

        Iterator<PageRange> it = pageSelection.iterator();
        while (it.hasNext()) {
            PageRange range = it.next();
            extractPageRange(lastPage, even, filteredPages, range);
        }
        return filteredPages;
    }

    private void extractPageRange(Integer lastPage, boolean even, Set<PageRange> filteredPages, PageRange range) {
        if (range.getEnd() != range.getStart()) {
            int startPage = range.getStart();
            //if range is specified as "7-", set the endPage to the last page of document
            int endPage = (lastPage < range.getEnd()) ? lastPage : range.getEnd();
            for (int number = startPage; number <= endPage; number++) {
                //only add even or odd numbers back into page collection depending on which the user selected
                if (((number % 2) == 0 && even) ||
                        ((number % 2) == 1 && !even)) {
                    filteredPages.add(new PageRange(number, number));
                }
            }
        }
        else {
            //only a single page not a range of pages
            filteredPages.add(range);
        }
    }

    boolean hasInput() {
        return !inputs.isEmpty();
    }

    @Override
    public void output(SingleOrMultipleTaskOutput output) {
        this.output = output;
    }

    @Override
    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    protected SingleOrMultipleTaskOutput getOutput() {
        return output;
    }

    protected String getPrefix() {
        return prefix;
    }

    public void rotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void rotationType(PredefinedSetOfPages predefinedRotationType) {
        this.predefinedRotationType = predefinedRotationType;

    }

    @Override
    public BulkRotateParameters build() {
        BulkRotateParameters params = new BulkRotateParameters();
        params.setCompress(isCompress());
        params.setExistingOutputPolicy(existingOutput());
        params.setVersion(getVersion());
        params.setOutput(getOutput());
        params.setOutputPrefix(getPrefix());
        inputs.forEach(params::addInput);
        return params;
    }

}
