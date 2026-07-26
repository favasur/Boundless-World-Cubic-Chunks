package com.typesafe.config.impl;

import java.util.Collection;

interface Unmergeable {
   Collection<? extends AbstractConfigValue> unmergedValues();
}
