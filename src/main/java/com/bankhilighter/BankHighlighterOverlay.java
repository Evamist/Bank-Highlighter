package com.bankhilighter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

class BankHighlighterOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;
	private final BankHighlighterPlugin plugin;
	private final BankHighlighterConfig config;
	private final Cache<Long, Image> fillCache;
	private final Cache<Integer, BankHighlighterTag> tagCache;
	private final BankHighlighterTag NONE = new BankHighlighterTag();

	@Inject
	private BankHighlighterOverlay(ItemManager itemManager, BankHighlighterPlugin plugin, BankHighlighterConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnBank();
		showOnInterfaces(
			InterfaceID.BANK_INVENTORY,
			InterfaceID.BANK_PIN
		);
		fillCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1)
			.maximumSize(32)
			.build();
		tagCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1)
			.maximumSize(39)
			.build();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		final BankHighlighterTag tag = getTag(itemId);
		if (tag == null || tag.color == null)
		{
			return;
		}

		final Color color = tag.color;

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (config.showTagOutline())
		{
			final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
			graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
		}

		if (config.showTagFill())
		{
			final Image image = getFillImage(color, widgetItem.getId(), widgetItem.getQuantity());
			graphics.drawImage(image, (int) bounds.getX(), (int) bounds.getY(), null);
		}

		if (config.showTagUnderline())
		{
			int heightOffSet = (int) bounds.getY() + (int) bounds.getHeight() + 2;
			graphics.setColor(color);
			graphics.drawLine((int) bounds.getX(), heightOffSet, (int) bounds.getX() + (int) bounds.getWidth(), heightOffSet);
		}
	}

	private BankHighlighterTag getTag(int itemId)
	{
		BankHighlighterTag tag = tagCache.getIfPresent(itemId);
		if (tag == null)
		{
			tag = plugin.getTag(itemId);
			if (tag == null)
			{
				tagCache.put(itemId, NONE);
				return null;
			}

			if (tag == NONE)
			{
				return null;
			}

			tagCache.put(itemId, tag);
		}
		return tag;
	}

	private Image getFillImage(Color color, int itemId, int qty)
	{
		long key = (((long) itemId) << 32) | qty;
		Image image = fillCache.getIfPresent(key);
		if (image == null)
		{
			final Color fillColor = ColorUtil.colorWithAlpha(color, config.fillOpacity());
			image = ImageUtil.fillImage(itemManager.getImage(itemId, qty, false), fillColor);
			fillCache.put(key, image);
		}
		return image;
	}

	void invalidateCache()
	{
		fillCache.invalidateAll();
		tagCache.invalidateAll();
	}
}
