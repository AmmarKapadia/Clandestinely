# Rule-Based Tip Library v1 — CuacaUM (M3 Smart Tips)

| | |
|---|---|
| **Owner** | Ammar |
| **Week** | 5 |
| **File** | `app/src/main/assets/rule_tips_v1.json` |
| **Tips** | 32 (English + Bahasa Melayu draft) |
| **Requirement** | FR3.3: fall back to a curated rule-based tip library when the AI service is unavailable |

The library is bundled inside the app, so it works offline (NFR2). It is also the safety net for the AI tips: if Gemini fails or the phone is offline, these tips are shown instead.

## 1. Coverage

Every weather state from `weather_rules.md` (plus UV) has at least one tip for everyone at every time of day, and every health sensitivity in the user profile has at least one tip.

| Weather state | Tips |
|---|---|
| Thunderstorm | 5 |
| Haze (unhealthy, AQI 151+) | 4 |
| Rain | 6 |
| Heat | 6 |
| Haze (sensitive groups, AQI 101–150) | 4 |
| UV (index 8+) | 3 |
| Normal | 4 |
| **Total** | **32** |

| Sensitivity | Tips |
|---|---|
| Asthma | 3 (haze) |
| Heat-sensitive | 1 (heat) |
| Limited mobility | 2 (rain, thunderstorm) |
| UV-sensitive | 1 (UV) |

Time-of-day tips: peak-hour traffic in the rain (morning, evening), the hottest part of the day (afternoon), and walking back late (evening, night).

## 2. How tips are picked

`RuleTipLibrary.select(states, sensitivities, timeSlot)` returns up to **3 tips**:

1. **Most dangerous state first:** thunderstorm → unhealthy haze → rain → heat → haze (sensitive) → UV → normal.
2. **For each active state:** the user's sensitivity tips first (e.g. asthma during haze), then that state's most important tip for everyone (priority 1).
3. **Fill the rest** with more tips for everyone, most dangerous state first.
4. Tips with the same priority **take turns each hour**, so the Home card doesn't show the same tip all day.
5. Tips marked with a time of day only appear then.

Examples (from the real library):

| Situation | Tips shown |
|---|---|
| Thunderstorm + rain, afternoon | `thunder-01`, `rain-01`, `thunder-02` |
| Haze (sensitive), morning, user has asthma | `hazesens-03`, `hazesens-01`, then a general haze tip |
| Rain, evening, user has limited mobility | `rain-05`, `rain-01`, then a general rain tip |
| Unhealthy haze, user has asthma | `hazeunh-04`, `hazeunh-01`, `hazeunh-02` |

## 3. All tips

Priority 1 = the tip that always shows for that state (for that group). Bahasa Melayu text is in the JSON file (draft, for Arieth to check).

| ID | Condition | For | Time | Priority | Tip (English) | Source |
|---|---|---|---|---|---|---|
| `thunder-01` | Thunderstorm | Everyone | Any | 1 | Thunderstorm nearby. As soon as you hear thunder, go inside a building. | NPS |
| `thunder-02` | Thunderstorm | Everyone | Any | 2 | Bus stops, pavilions and open walkways don't protect you from lightning. Wait inside an enclosed building. | NPS |
| `thunder-03` | Thunderstorm | Everyone | Any | 2 | Stay away from tall, isolated trees, open fields and lakes until the storm passes. | NPS |
| `thunder-04` | Thunderstorm | Everyone | Any | 2 | Wait at least 30 minutes after the last thunder before heading out again, even if the rain has stopped. | NPS |
| `thunder-05` | Thunderstorm | Limited mobility | Any | 1 | If you can't reach a building quickly, don't shelter under a tree. Ask someone nearby for help, or call UM Security from the Emergency tab. | NPS |
| `hazeunh-01` | Haze (unhealthy, AQI 151+) | Everyone | Any | 1 | Air quality is unhealthy for everyone. Avoid outdoor activities and stay indoors with the windows closed. | MOH |
| `hazeunh-02` | Haze (unhealthy, AQI 151+) | Everyone | Any | 2 | If you must go outside, wear an N95 mask. Three-ply masks give only limited protection against haze. | MOH |
| `hazeunh-03` | Haze (unhealthy, AQI 151+) | Everyone | Any | 2 | Drink plenty of water, and see a doctor if you have breathing difficulty or chest pain. | Team |
| `hazeunh-04` | Haze (unhealthy, AQI 151+) | Asthma | Any | 1 | The air is unhealthy and you have asthma. Stay indoors as much as possible and keep your reliever inhaler close. | MOH |
| `rain-01` | Rain | Everyone | Any | 1 | Rain is likely. Bring an umbrella and use the Driest route to stay under covered walkways. | Team |
| `rain-02` | Rain | Everyone | Any | 2 | Don't walk through flowing water. Just 15 cm of moving water can make you fall. Turn back and find another way. | PUB |
| `rain-03` | Rain | Everyone | Any | 2 | Floors and stairs get slippery at building entrances. Slow down and use the handrails. | Team |
| `rain-04` | Rain | Everyone | morning, evening | 2 | Rain at peak hours slows traffic and buses around campus. Leave earlier, or wait it out indoors. | Team |
| `rain-05` | Rain | Limited mobility | Any | 1 | Paths may be slippery or flooded. Use the Driest route, avoid ramps with standing water, and allow extra time. | Team |
| `rain-06` | Rain | Everyone | Any | 3 | Seen a flooded path or fallen branch? Report it in the app so others can avoid it. | Team |
| `heat-01` | Heat | Everyone | Any | 1 | It feels like 38 °C or more. Drink water before you feel thirsty and refill your bottle. | Team |
| `heat-02` | Heat | Everyone | Any | 2 | Take the Shadiest route and walk at an easy pace. Rest in air-conditioned buildings along the way. | Team |
| `heat-03` | Heat | Everyone | Any | 2 | Dizzy, nauseous or sweating heavily? Move somewhere cool, loosen your clothing and sip cool water. | MDH |
| `heat-04` | Heat | Everyone | Any | 2 | Confusion, slurred speech or fainting can mean heatstroke. Call 999 straight away and cool the person down. | MDH |
| `heat-05` | Heat | Heat-sensitive | Any | 1 | Heat affects you more. Avoid walking outdoors in the hottest part of the day, and carry water and a small towel. | Team |
| `heat-06` | Heat | Everyone | afternoon | 2 | This is the hottest part of the day. Save long walks for the morning or evening if you can. | Team |
| `hazesens-01` | Haze (sensitive groups, AQI 101–150) | Everyone | Any | 1 | Air quality is unhealthy for sensitive groups. Keep time outdoors short and stay indoors when you can. | MOH |
| `hazesens-02` | Haze (sensitive groups, AQI 101–150) | Everyone | Any | 2 | Skip outdoor exercise today. Indoor activities are a better choice. | MOH |
| `hazesens-03` | Haze (sensitive groups, AQI 101–150) | Asthma | Any | 1 | Haze can trigger asthma. Keep your inhaler with you and wear an N95 mask outdoors. | MOH |
| `hazesens-04` | Haze (sensitive groups, AQI 101–150) | Asthma | Any | 2 | Wheezing, chest tightness or breathlessness getting worse? Go indoors, follow your asthma action plan, and get medical help if it doesn't improve. | Team |
| `uv-01` | UV (index 8+) | Everyone | Any | 1 | UV is very high. Avoid direct sun around midday and walk in the shade. | WHO |
| `uv-02` | UV (index 8+) | Everyone | Any | 2 | Outdoors for a while? Wear a hat, sunglasses and sunscreen, and reapply sunscreen after sweating. | WHO |
| `uv-03` | UV (index 8+) | UV-sensitive | Any | 1 | Strong UV can worsen sun-sensitive skin conditions. Cover up, use sunscreen and take the Shadiest route. | WHO |
| `normal-01` | Normal | Everyone | Any | 1 | No weather warnings on campus right now. Still carry a water bottle when walking between buildings. | Team |
| `normal-02` | Normal | Everyone | morning, afternoon | 2 | Weather in KL can change quickly in the afternoon. Keep a small umbrella in your bag. | Team |
| `normal-03` | Normal | Everyone | evening, night | 2 | Walking back late? Stick to well-lit paths and keep your phone charged. UM emergency contacts are in the Emergency tab. | Team |
| `normal-04` | Normal | Everyone | Any | 2 | Check the hourly forecast before a long walk across campus. Rain and heat can build up within an hour. | Team |

## 4. How the app uses it

```text
// Tips screen / Home card (M3), when Gemini isn't available:
List<String> states = ...;                                   // from the M1 weather classifier
List<String> sensitivities = TipCacheKey.sensitivitiesOf(profile.getHealth());
String slot = TipCacheKey.timeSlotAt(System.currentTimeMillis());

RuleTipLibrary library = RuleTipLoader.get(context);
List<RuleTip> tips = library.select(states, sensitivities, slot);
String text = RuleTipLibrary.combine(tips, profile.getSettings().getLanguage());
```

Full flow (schema section 5.4): saved tip in `tips_cache` → otherwise Gemini → if that fails, this library.

## 5. Editing the library

- Open `app/src/main/assets/rule_tips_v1.json` and add or change an entry. Each tip has: `id`, `state`, `sensitivity` (or `null`), `timeSlots` (empty = any time), `priority`, `en`, `ms`, `source`.
- Keep tips under 160 characters so they fit on the Home card.
- Run the unit tests afterwards (`TipLibraryFileTest` checks the file).

## 6. Next version (v2)

- Adjust tips to the survey results (most common pain points and routes).
- Replace "UM Security from the Emergency tab" wording once Arieth's emergency contacts are in the app.
- Arieth to review the Bahasa Melayu text.

## Sources

- **NPS:** [US National Park Service: Lightning Safety](https://home.nps.gov/articles/lightning-safety.htm)
- **MDH:** [Minnesota Department of Health: Know the signs of heat-related illness](https://www.health.state.mn.us/communities/environment/climate/docs/knowthesigns.pdf)
- **MOH:** [MOH: mask up, avoid outdoor activity as haze blankets Malaysia (Malay Mail, 16 Oct 2026)](https://malaymail.com/news/malaysia/2026/10/16/moh-says-mask-up-avoid-outdoor-activity-as-haze-blankets-malaysia-with-unhealthy-air/239285)
- **PUB:** [Singapore PUB: Flood public advisory](https://info.pub.gov.sg/drainage/floodmanagement/publicadvisory)
- **WHO:** [WHO: The ultraviolet (UV) index](https://www.who.int/news-room/q-a-detail/radiation-the-ultraviolet-(uv)-index)
- **Team:** Team-written general advice
