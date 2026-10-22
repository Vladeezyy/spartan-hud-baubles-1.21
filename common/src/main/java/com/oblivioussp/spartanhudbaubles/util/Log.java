package com.oblivioussp.spartanhudbaubles.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

/**
 * Basic logger
 * @author ObliviousSpartan
 *
 */
public class Log
{
	protected static final Logger LOGGER = LogManager.getLogger(ModSpartanHUDBaubles.NAME);

	public static void debug(Object object)
	{
		LOGGER.debug(object);
	}

	public static void error(Object object)
	{
		LOGGER.error(object);
	}

	public static void error(String message, Throwable throwable)
	{
		LOGGER.error(message, throwable);
	}

	public static void fatal(Object object)
	{
		LOGGER.fatal(object);
	}

	public static void info(Object object)
	{
		LOGGER.info(object);
	}

	public static void trace(Object object)
	{
		LOGGER.trace(object);
	}

	public static void warn(Object object)
	{
		LOGGER.warn(object);
	}
}
