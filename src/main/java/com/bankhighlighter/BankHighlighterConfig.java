package com.bankhighlighter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BankHighlighterConfig.GROUP)
public interface BankHighlighterConfig extends Config
{
	String GROUP = "com/bankhighlighter";

	@ConfigSection(
		name = "Tag display mode",
		description = "How tags are displayed in the bank",
		position = 0
	)
	String tagStyleSection = "tagStyleSection";

	@ConfigItem(
		position = 0,
		keyName = "showTagOutline",
		name = "Outline",
		description = "Configures whether or not item tags should be outlined",
		section = tagStyleSection
	)
	default boolean showTagOutline()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "tagUnderline",
		name = "Underline",
		description = "Configures whether or not item tags should be underlined",
		section = tagStyleSection
	)
	default boolean showTagUnderline()
	{
		return false;
	}

	@ConfigItem(
		position = 2,
		keyName = "tagFill",
		name = "Fill",
		description = "Configures whether or not item tags should be filled",
		section = tagStyleSection
	)
	default boolean showTagFill()
	{
		return false;
	}

	@Range(
		max = 255
	)
	@ConfigItem(
		position = 3,
		keyName = "fillOpacity",
		name = "Fill opacity",
		description = "Configures the opacity of the tag \"Fill\"",
		section = tagStyleSection
	)
	default int fillOpacity()
	{
		return 50;
	}
}
