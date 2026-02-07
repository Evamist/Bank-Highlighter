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
		name = "Menu",
		description = "Menu entry options",
		position = 0
	)
	String menuSection = "menuSection";

	@ConfigSection(
		name = "Tag display mode",
		description = "How tags are displayed in the bank",
		position = 1
	)
	String tagStyleSection = "tagStyleSection";

	@ConfigSection(
		name = "Integration",
		description = "Integrations with other plugins",
		position = 2
	)
	String integrationSection = "integrationSection";

	@ConfigItem(
		position = 0,
		keyName = "requireShiftForMenu",
		name = "Require Shift",
		description = "Require holding Shift to show Bank Highlight menu entries.",
		section = menuSection
	)
	default boolean requireShiftForMenu()
	{
		return false;
	}

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

	@ConfigItem(
		keyName = "useInventoryTagsConfig",
		name = "Share with Inventory Tags",
		description = "Use the Inventory Tags plugin config for highlight colors so you don't need to tag items twice.",
		position = 0,
		section = integrationSection
	)
	default boolean useInventoryTagsConfig()
	{
		return false;
	}
}
