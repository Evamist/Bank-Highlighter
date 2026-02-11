package com.bankhighlighter;

import java.awt.Color;

/**
 * Persisted highlight tag for an item.
 */
public class BankHighlighterTag
{
	/**
	 * Package-private for Gson + existing overlay code.
	 */
	Color color;

	public Color getColor()
	{
		return color;
	}

	public void setColor(Color color)
	{
		this.color = color;
	}
}
