package com.bankhighlighter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.widgets.WidgetUtil;
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
		showOnInventory();
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
		final Widget widget = widgetItem.getWidget();
		if (widget != null)
		{
			final int iface = WidgetUtil.componentToInterface(widget.getId());
			final boolean isBankIface = iface == InterfaceID.BANKMAIN || iface == InterfaceID.BANKSIDE;
			if (!isBankIface && !plugin.isBankOpen())
			{
				return;
			}
		}

		final int resolvedItemId = itemId > 0 ? itemId : widgetItem.getId();
		if (resolvedItemId <= 0)
		{
			return;
		}

		final BankHighlighterTag tag = getTag(resolvedItemId);
		if (tag == null || tag.color == null)
		{
			return;
		}

		final Color color = tag.color;
		int quantity = widgetItem.getQuantity();
		if (quantity <= 0)
		{
			quantity = 1;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		if (config.showTagOutline())
		{
			final BufferedImage outline = itemManager.getItemOutline(resolvedItemId, quantity, color);
			if (outline != null)
			{
				graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
			}
		}

		if (config.showTagFill())
		{
			final Image image = getFillImage(color, resolvedItemId, quantity);
			if (image != null)
			{
				graphics.drawImage(image, (int) bounds.getX(), (int) bounds.getY(), null);
			}
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
