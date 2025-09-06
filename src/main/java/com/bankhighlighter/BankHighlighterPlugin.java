package com.bankhighlighter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.applet.Applet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import static net.runelite.api.InventoryID.BANK;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
	name = "Bank Highlighter",
	description = "About\n" +
			"Lets you color-code items in your bank or inventory (while the bank is open) for easier visibility. This feature is particularly useful for organizing skill/combat gear sets and quickly finding the items you need. Built from the original \"Inventory Tags,\" coding.",
	tags = {"highlight", "items", "overlay", "tagging", "bank"}
)
@Slf4j
public class BankHighlighterPlugin extends Plugin
{
	private static final String TAG_KEY_PREFIX = "tag_";

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BankHighlighterOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Provides
	BankHighlighterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankHighlighterConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	BankHighlighterTag getTag(int itemId)
	{
		String tag = configManager.getConfiguration(BankHighlighterConfig.GROUP, TAG_KEY_PREFIX + itemId);
		if (tag == null || tag.isEmpty())
		{
			return null;
		}

		return gson.fromJson(tag, BankHighlighterTag.class);
	}

	void setTag(int itemId, BankHighlighterTag tag)
	{
		String json = gson.toJson(tag);
		configManager.setConfiguration(BankHighlighterConfig.GROUP, TAG_KEY_PREFIX + itemId, json);
		overlay.invalidateCache();
	}

	void unsetTag(int itemId)
	{
		configManager.unsetConfiguration(BankHighlighterConfig.GROUP, TAG_KEY_PREFIX + itemId);
		overlay.invalidateCache();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals(BankHighlighterConfig.GROUP))
		{
			overlay.invalidateCache();
		}
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event)
	{
		if (!client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();

		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();
			if (w == null)
			{
				continue;
			}

			final int iface = WidgetUtil.componentToInterface(w.getId());
			final boolean isBankIface = iface == InterfaceID.BANK || iface == InterfaceID.BANK_INVENTORY;
			if (!isBankIface)
			{
				continue;
			}

			final int itemId = entry.getItemId() > 0 ? entry.getItemId() : w.getItemId();
			if (itemId <= 0)
			{
				continue;
			}

			final BankHighlighterTag tag = getTag(itemId);

			final MenuEntry parent = client.createMenuEntry(idx)
				.setOption("Bank Highlight")
				.setTarget(entry.getTarget())
				.setType(MenuAction.RUNELITE);

			final Menu submenu = parent.createSubMenu();

			Set<Color> bankColors = new HashSet<>(getColorsFromItemContainer(BANK));
			for (Color color : bankColors)
			{
				if (tag == null || !color.equals(tag.getColor()))
				{
					submenu.createMenuEntry(0)
						.setOption(ColorUtil.prependColorTag("Color", color))
						.setType(MenuAction.RUNELITE)
						.onClick(e ->
						{
							BankHighlighterTag t = new BankHighlighterTag();
							t.setColor(color);
							setTag(itemId, t);
							overlay.invalidateCache();
						});
				}
			}

			// Manual picker
			submenu.createMenuEntry(0)
				.setOption("Pick")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					Color base = (tag == null || tag.getColor() == null) ? Color.WHITE : tag.getColor();
					SwingUtilities.invokeLater(() ->
					{
						RuneliteColorPicker colorPicker = colorPickerManager.create(
							SwingUtilities.windowForComponent((Applet) client),
							base,
							"Bank Highlight",
							true
						);
						colorPicker.setOnClose(c ->
						{
							BankHighlighterTag t = new BankHighlighterTag();
							t.setColor(c);
							setTag(itemId, t);
							overlay.invalidateCache();
						});
						colorPicker.setVisible(true);
					});
				});

			// Reset existing tag
			if (tag != null)
			{
				submenu.createMenuEntry(0)
					.setOption("Reset")
					.setType(MenuAction.RUNELITE)
					.onClick(e ->
					{
						unsetTag(itemId);
						overlay.invalidateCache();
					});
			}
		}
	}

	private List<Color> getColorsFromItemContainer(InventoryID inventoryID)
	{
		List<Color> colors = new ArrayList<>();
		ItemContainer container = client.getItemContainer(inventoryID);
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				BankHighlighterTag tag = getTag(item.getId());
				if (tag != null && tag.color != null)
				{
					if (!colors.contains(tag.color))
					{
						colors.add(tag.color);
					}
				}
			}
		}
		return colors;
	}
}
