import { ItemType } from "../data/models";

// Replicates DefaultActivityColorHex from ActivityColorPrefs.kt
export const ITEM_TYPE_COLORS: Record<ItemType, string> = {
  [ItemType.FLIGHT]: "#7594CA",
  [ItemType.HOTEL]: "#405F91",
  [ItemType.RESTAURANT]: "#F9AD00",
  [ItemType.ATTRACTION]: "#8E44AD",
  [ItemType.ACTIVITY]: "#10B981",
  [ItemType.TRANSPORT]: "#43474F",
  [ItemType.SHOW]: "#BA1A1A",
  [ItemType.CUSTOM]: "#747780",
};

export const getAccentColor = (type: ItemType): string => {
  return ITEM_TYPE_COLORS[type] || ITEM_TYPE_COLORS[ItemType.CUSTOM];
};
