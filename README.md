# WLED Holiday Lights Controller for Hubitat

A Hubitat Elevation app that automatically controls WLED lights based on holidays and schedules. Configure different color schemes for each holiday, with support for different colors on weekdays vs weekends.

## Features

- **Multiple WLED Controllers**: Add and manage multiple WLED controller endpoints
- **Custom Color Definitions**: Define colors with names and RGB values
- **Holiday Configuration**: Set up unlimited holidays with date ranges
- **Day-of-Week Scheduling**: Different colors for weekdays vs weekends, or configure each day individually
- **Master On/Off Schedule**: Set daily on/off times for all lights
- **Priority System**: Handle overlapping holidays with configurable priorities
- **Manual Control**: Trigger lights manually or turn them off on demand
- **Async HTTP**: Non-blocking POST requests to WLED controllers

## Installation

1. In Hubitat, go to **Apps Code**
2. Click **+ New App**
3. Copy and paste the contents of `wled-holiday-lights.groovy`
4. Click **Save**
5. Go to **Apps** and click **+ Add User App**
6. Select **WLED Holiday Lights Controller**

## Configuration

### Step 1: Add WLED Controllers

1. Click on "Configure WLED Controllers"
2. Set the number of controllers you have
3. For each controller, provide:
   - **Name**: A friendly name (e.g., "Front Porch", "Roofline")
   - **Endpoint URL**: The WLED JSON API endpoint (e.g., `http://192.168.1.123/json/state`)
   - **Enabled**: Toggle to enable/disable the controller

### Step 2: Define Colors

1. Click on "Define Colors"
2. Set the number of colors you want to define
3. For each color, provide:
   - **Name**: A friendly name (e.g., "Christmas Red", "Halloween Orange")
   - **Red/Green/Blue**: RGB values (0-255)

Example colors:
| Color | R | G | B |
|-------|---|---|---|
| Red | 255 | 0 | 0 |
| Green | 0 | 255 | 0 |
| Blue | 0 | 0 | 255 |
| White | 255 | 255 | 255 |
| Warm White | 255 | 200 | 150 |
| Orange | 255 | 165 | 0 |
| Purple | 128 | 0 | 128 |
| Yellow | 255 | 255 | 0 |
| Pink | 255 | 105 | 180 |

### Step 3: Configure Base JSON Payload

The default payload is:
```json
{"on":true,"bri":33,"transition":7,"ps":1,"seg":[{"id":0,"col":[[255,0,0],[0,255,0],[0,0,0]],"fx":51,"sx":65,"ix":255}]}
```

You can customize:
- `bri`: Brightness (0-255)
- `transition`: Transition time
- `ps`: Preset number
- `fx`: Effect number
- `sx`: Effect speed
- `ix`: Effect intensity

The `col` array will be replaced with your holiday colors automatically.

### Step 4: Set Up Holidays

1. Click on "Configure Holidays"
2. Set the number of holidays
3. For each holiday, configure:
   - **Name**: Holiday name (e.g., "Christmas")
   - **Enabled**: Toggle to enable/disable
   - **Start/End Date**: Date range for the holiday
   - **Priority**: Lower number = higher priority (for overlapping dates)

4. Click "Configure Colors for [Holiday Name]" to set colors:
   - **Weekday Colors**: Primary, secondary, and tertiary colors for Monday-Friday
   - **Weekend Colors**: Primary, secondary, and tertiary colors for Saturday-Sunday
   - **Individual Days**: Optionally configure each day separately

### Step 5: Set Master Schedule

- **Lights Turn On Time**: When lights should turn on daily
- **Lights Turn Off Time**: When lights should turn off daily
- **Enable Automatic Scheduling**: Toggle to enable/disable the schedule

## Example Setup: Christmas

1. **Colors**:
   - Color 1: "Christmas Red" - 255, 0, 0
   - Color 2: "Christmas Green" - 0, 255, 0
   - Color 3: "Warm White" - 255, 200, 150

2. **Holiday**:
   - Name: "Christmas"
   - Start: December 1
   - End: January 1
   - Priority: 10

3. **Colors for Christmas**:
   - Weekday: Red (primary), Green (secondary), Black (tertiary)
   - Weekend: Warm White (primary), Warm White (secondary), Black (tertiary)

4. **Schedule**:
   - Turn On: 5:00 PM
   - Turn Off: 11:00 PM

## How It Works

1. At the scheduled turn-on time, the app checks if today falls within any active holiday date range
2. If multiple holidays are active, the one with the lowest priority number is selected
3. The app determines if today is a weekday or weekend
4. Colors are retrieved based on the day configuration
5. The base JSON payload is modified to include the selected colors
6. A POST request is sent to each enabled WLED controller

## Troubleshooting

- **Enable Debug Logging**: Turn on debug logging to see detailed information in the Hubitat logs
- **Check Endpoint URLs**: Ensure URLs include the protocol (http://) and full path (/json/state)
- **Test Manually**: Use the "Trigger Lights Now" button to test your configuration
- **Verify WLED**: Open the WLED web interface directly to ensure the controller is responding

## WLED API Reference

The app uses the WLED JSON API. For more information, see:
- [WLED JSON API Documentation](https://kno.wled.ge/interfaces/json-api/)

## License

MIT License - Feel free to use, modify, and distribute.
