/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.search;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.as.console.client.widgets.progress.ProgressElement;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;

import java.util.*;

import static com.google.gwt.dom.client.Style.Unit.PCT;
import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Harald Pehl
 */
class SearchPopup extends DefaultWindow {

    private static final TokenGroupTemplate TEMPLATE = GWT.create(TokenGroupTemplate.class);

    private final Harvest harvest;
    private final Index index;

    private final DeckPanel deck;
    private final ProgressElement progressBar;
    private final DefaultCellList<TokenGroup> resultList;
    private final SingleSelectionModel<TokenGroup> resultSelectionModel;
    private final ListDataProvider<TokenGroup> resultProvider;
    private final TextBox textBox;
    private final HTML numberOfResults;
    private final PlaceManager placeManager;

    SearchPopup(final Harvest harvest, final Index index, final PlaceManager placeManager) {
        super("Search");
        addStyleName("hal-searchPopup");

        this.harvest = harvest;
        this.index = index;
        this.placeManager = placeManager;

        deck = new DeckPanel();
        VerticalPanel indexPanel = new VerticalPanel();
        indexPanel.setStyleName("fill-layout-width");
        indexPanel.getElement().getStyle().setPadding(2, Style.Unit.PX);
        indexPanel.add(new HTML(Console.MESSAGES.search_index_pending()));
        progressBar = new ProgressElement();
        indexPanel.add(progressBar);
        deck.add(indexPanel);

        VerticalPanel searchPanel = new VerticalPanel();
        searchPanel.setStyleName("window-content");

        searchPanel.add(new HTML("<h3>" + Console.CONSTANTS.search_popup_title() + "</h3>"));
        searchPanel.add(new Label(Console.MESSAGES.search_popup_description(SearchTool.getShortcut())));

        textBox = new TextBox();
        textBox.getElement().getStyle().setWidth(66, PCT);
        textBox.getElement().setAttribute("placeholder", Console.CONSTANTS.search_placeholder());
        textBox.getElement().getStyle().setMarginTop(15, PX);
        textBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                if (keyUpEvent.isDownArrow() && !resultProvider.getList().isEmpty()) {
                    resultList.setFocus(true);
                } else {
                    executeQuery();
                }
            }
        });

        textBox.setTabIndex(0);
        searchPanel.add(textBox);

        numberOfResults = new HTML();
        numberOfResults.addStyleName("no-results");
        numberOfResults.setVisible(false);
        searchPanel.add(numberOfResults);

        resultList = new DefaultCellList<TokenGroup>(new ResultCell());
        resultList.addCellPreviewHandler(new CellPreviewEvent.Handler<TokenGroup>() {
            @Override
            public void onCellPreview(CellPreviewEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    resultSelectionModel.setSelected(
                            resultProvider.getList().get(resultList.getKeyboardSelectedRow()), true);
                }
            }
        });

        resultSelectionModel = new SingleSelectionModel<TokenGroup>();
        resultSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                navigate();
            }
        });

        resultList.setSelectionModel(resultSelectionModel);
        resultList.addStyleName("fill-layout-width");
        resultList.addStyleName("search-result");

        resultProvider = new ListDataProvider<TokenGroup>();
        resultProvider.addDataDisplay(resultList);
        searchPanel.add(resultList);

        deck.add(searchPanel);
        deck.showWidget(1);
        resultList.setTabIndex(0);
        setWidget(new ScrollPanel(deck));
    }

    private void navigate() {
        TokenGroup selection = resultSelectionModel.getSelectedObject();
        if (selection != null) {
            hide();
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(selection.getToken()).build());
        }
    }

    private void executeQuery() {
        String query = textBox.getText().trim();
        if (query.length() != 0) {
            // split the query into multiple terms
            String[] terms = query.split(" ");

            // collect the hits over all terms into token groups (using disjunction)
            Map<String, TokenGroup> tokenGroups = new LinkedHashMap<>();
            for (String term : terms) {
                if (term != null && term.trim().length() > 1) {
                    List<Document> hits = index.search(term);
                    for (Document hit : hits) {
                        String token = hit.getToken();
                        TokenGroup tokenGroup = tokenGroups.get(token);
                        if (tokenGroup == null) {
                            tokenGroup = new TokenGroup(token);
                            tokenGroups.put(token, tokenGroup);
                        }
                        tokenGroup.add(hit);
                    }
                }
            }

            // display token groups
            ArrayList<TokenGroup> results = new ArrayList<TokenGroup>(tokenGroups.values());
            numberOfResults.setVisible(true);
            if (results.isEmpty()) {
                numberOfResults.removeStyleName("number-of-results");
                numberOfResults.addStyleName("no-results");
                numberOfResults.setHTML(Console.CONSTANTS.search_no_results());
            } else {
                numberOfResults.removeStyleName("no-results");
                numberOfResults.addStyleName("number-of-results");
                numberOfResults.setHTML(Console.MESSAGES.search_n_results(results.size()));
            }
            resultProvider.setList(results);
            resultProvider.refresh();
        } else {
            // clear display
            numberOfResults.setVisible(false);
            resultProvider.setList(Collections.<TokenGroup>emptyList());
            resultProvider.refresh();
        }
    }

    void showIndexPage() {
        deck.showWidget(0);
    }

    void index() {
        harvest.run(new FilterDuplicatesHarvest(index, this), progressBar);
    }

    void showSearchPage() {
        deck.showWidget(1);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                textBox.setFocus(true);
            }
        });
    }


    public class ResultCell extends AbstractCell<TokenGroup> {

        @Override
        public void render(Context context, TokenGroup tokenGroup, SafeHtmlBuilder safeHtmlBuilder) {
            SafeHtmlBuilder descriptions = new SafeHtmlBuilder();
            for (String description : tokenGroup.getDescriptions()) {
                descriptions.append(TEMPLATE.description(description));
            }
            String token = tokenGroup.getToken();
            try {
                // try to find a description for this token
                token = Console.TOKENS.getString(token.replace('-', '_'));
            } catch (MissingResourceException e) {
                System.err.println("No description found for token " + token + ": " + e.getMessage());
            }
            if (tokenGroup.getKeywords().isEmpty()) {
                safeHtmlBuilder.append(TEMPLATE.tokenGroup(token, descriptions.toSafeHtml()));
            } else {
                SafeHtmlBuilder keywords = new SafeHtmlBuilder();
                for (String keyword : tokenGroup.getKeywords()) {
                    keywords.append(TEMPLATE.keyword(keyword));
                }
                safeHtmlBuilder.append(TEMPLATE.tokenGroup(token, descriptions.toSafeHtml(), keywords.toSafeHtml()));
            }
        }
    }

    public interface TokenGroupTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"search-token-group\">" +
                "<div class=\"header\"><i class=\"icon-file-alt\"></i>&nbsp;{0}</div>" +
                "<ul class=\"descriptions\">{1}</ul>" +
                "</div>")
        SafeHtml tokenGroup(String header, SafeHtml descriptions);

        @Template("<div class=\"search-token-group\">" +
                "<div class=\"header\"><i class=\"icon-file-alt\"></i>&nbsp;{0}</div>" +
                "<ul class=\"descriptions\">{1}</ul>" +
                "<div class=\"keywords\">{2}</div>" +
                "</div>")
        SafeHtml tokenGroup(String header, SafeHtml descriptions, SafeHtml keywords);

        @Template("<li>{0}</li>")
        SafeHtml description(String description);

        @Template("<span>{0}</span>")
        SafeHtml keyword(String keyword);
    }
}
