package scrum.client;

import ilarkesto.gwt.client.Gwt;
import ilarkesto.gwt.client.RichtextFormater;
import scrum.client.collaboration.Wikipage;
import scrum.client.common.AScrumComponent;
import scrum.client.wiki.EntityReferencePlugin;
import scrum.client.wiki.WikiModel;
import scrum.client.wiki.WikiParser;

public class Wiki extends AScrumComponent implements RichtextFormater {

	@Override
	protected void onInitialization() {
		super.onInitialization();
		Gwt.setDefaultRichtextFormater(this);
	}

	public String getTemplate(String name) {
		Wikipage page = getCurrentProject().getWikipage("template:" + name);
		return page == null ? null : page.getText();
	}

	public String richtextToHtml(String text) {
		if (Gwt.isEmpty(text)) return text;
		return toHtml(text);
	}

	public static String toHtml(String wiki) {
		WikiParser parser = new WikiParser(wiki);
		parser.addPlugin(new EntityReferencePlugin());
		WikiModel model = parser.parse();
		return model.toHtml();
	}

}
