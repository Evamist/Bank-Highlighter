package com.bankhighlighter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
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

@Slf4j
@PluginDescriptor(
	name = "Bank Highlighter",
	description = "Adds customizable highlighting to items in the bank"
)
public class BankHighlighterPlugin extends Plugin
{
	private static final String TAG_KEY_PREFIX = "tag_";
	private static final String INVENTORY_TAGS_GROUP = "inventorytags";

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
	private BankHighlighterConfig config;

	@Inject
	private ColorPickerManager colorPickerManager;

	private boolean bankOpen;

	@Provides
	BankHighlighterConfig getConfig(ConfigManager configManager)
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

	public void setTag(int itemId, BankHighlighterTag tag)
	{
		String key = TAG_KEY_PREFIX + itemId;
		String value = gson.toJson(tag);
		configManager.setConfiguration(getConfigGroup(), key, value);
	}

	public void unsetTag(int itemId)
	{
		String key = TAG_KEY_PREFIX + itemId;
		configManager.unsetConfiguration(getConfigGroup(), key);
	}

	public BankHighlighterTag getTag(int itemId)
	{
		String key = TAG_KEY_PREFIX + itemId;
		String json = configManager.getConfiguration(getConfigGroup(), key);
		if (json == null)
		{
			return null;
		}
		return gson.fromJson(json, BankHighlighterTag.class);
	}

	private String getConfigGroup()
	{
		return config.useInventoryTagsConfig() ? INVENTORY_TAGS_GROUP : BankHighlighterConfig.GROUP;
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event)
	{
		if (event.getGroup().equals(BankHighlighterConfig.GROUP)
			|| (config.useInventoryTagsConfig() && event.getGroup().equals(INVENTORY_TAGS_GROUP)))
		{
			overlay.invalidateCache();
		}
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event)
	{
		if (config.requireShiftForMenu() && !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();
		final Set<Long> seenItems = new HashSet<>();
		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();
			if (w == null)
			{
				continue;
			}

			final int iface = WidgetUtil.componentToInterface(w.getId());
			final boolean isBankIface = iface == InterfaceID.BANKMAIN || iface == InterfaceID.BANKSIDE;
			if (!isBankIface && !bankOpen)
			{
				continue;
			}

			final int itemId = entry.getItemId() > 0 ? entry.getItemId() : w.getItemId();
			if (itemId <= 0)
			{
				continue;
			}

			final long slotKey = (((long) w.getId()) << 32) ^ (itemId & 0xffffffffL);
			if (!seenItems.add(slotKey))
			{
				continue;
			}

			final BankHighlighterTag tag = getTag(itemId);

			final MenuEntry parent = client.getMenu().createMenuEntry(idx)
				.setOption("Bank Highlight")
				.setTarget(entry.getTarget())
				.setType(MenuAction.RUNELITE);

			final Menu submenu = parent.createSubMenu();

			Set<Color> bankColors = new HashSet<>(getColorsFromItemContainer(InventoryID.BANK));
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
							SwingUtilities.windowForComponent(client.getCanvas()),
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

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = false;
		}
	}

	boolean isBankOpen()
	{
		return bankOpen;
	}

	private List<Color> getColorsFromItemContainer(final int inventoryId)
	{
		List<Color> colors = new ArrayList<>();
		final ItemContainer container = client.getItemContainer(inventoryId);
		if (container == null)
		{
			return colors;
		}

		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}

			final BankHighlighterTag tag = getTag(item.getId());
			if (tag != null && tag.getColor() != null && !colors.contains(tag.getColor()))
			{
				colors.add(tag.getColor());
			}
		}
		return colors;
	}
}
