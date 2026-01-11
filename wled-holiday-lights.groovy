/**
 * WLED Holiday Lights Controller
 *
 * A Hubitat app to control WLED lights based on holidays and schedules.
 * Supports multiple WLED controllers, custom colors, holiday date ranges,
 * and day-of-week color configurations.
 *
 * Author: Claude AI
 * Date: 2026-01-11
 */

definition(
    name: "WLED Holiday Lights Controller",
    namespace: "wled-holiday",
    author: "Claude AI",
    description: "Control WLED lights based on holidays with customizable colors and schedules",
    category: "Lighting",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage", title: "WLED Holiday Lights Controller", install: true, uninstall: true) {
        section("WLED Controllers") {
            href "controllersPage", title: "Configure WLED Controllers", description: getControllersDescription()
        }
        section("Power Switches") {
            input "powerSwitches", "capability.switch", title: "Switches to turn on before activating WLED",
                description: "Select switches that control power to WLED controllers", multiple: true, required: false
            input "powerSwitchDelay", "number", title: "Delay after turning on switches (seconds)",
                description: "Time to wait for WLED controllers to boot up", defaultValue: 20, range: "0..120"
            input "turnOffPowerSwitches", "bool", title: "Turn off power switches when lights turn off",
                description: "Also turn off the power switches when the lights are turned off", defaultValue: false
        }
        section("Color Definitions") {
            href "colorsPage", title: "Define Colors", description: getColorsDescription()
        }
        section("Base JSON Payload") {
            input "basePayload", "text", title: "Base JSON Payload", required: true,
                defaultValue: '{"on":true,"bri":33,"transition":7,"ps":1,"seg":[{"id":0,"col":[[255,0,0],[0,255,0],[0,0,0]],"fx":51,"sx":65,"ix":255}]}',
                description: "The base WLED JSON payload. Colors will be replaced based on holiday settings."
        }
        section("Holidays") {
            href "holidaysPage", title: "Configure Holidays", description: getHolidaysDescription()
        }
        section("Master Schedule") {
            input "turnOnTime", "time", title: "Lights Turn On Time", required: true
            input "turnOffTime", "time", title: "Lights Turn Off Time", required: true
            input "enableSchedule", "bool", title: "Enable Automatic Scheduling", defaultValue: true
        }
        section("Manual Control") {
            input "manualTrigger", "button", title: "Trigger Lights Now"
            input "manualOff", "button", title: "Turn Off Lights Now"
        }
        section("Logging") {
            input "enableDebugLog", "bool", title: "Enable Debug Logging", defaultValue: true
        }
    }

    // Controllers Page
    page(name: "controllersPage", title: "WLED Controllers") {
        section("Add/Edit WLED Controllers") {
            input "controllerCount", "number", title: "Number of WLED Controllers", range: "1..20", defaultValue: 1, submitOnChange: true
        }
        if (controllerCount) {
            for (int i = 1; i <= controllerCount; i++) {
                section("Controller ${i}") {
                    input "controllerName${i}", "text", title: "Controller Name", defaultValue: "Controller ${i}"
                    input "controllerEndpoint${i}", "text", title: "WLED Endpoint URL",
                        description: "e.g., http://192.168.1.123/json/state"
                    input "controllerEnabled${i}", "bool", title: "Enabled", defaultValue: true
                }
            }
        }
    }

    // Colors Page
    page(name: "colorsPage", title: "Color Definitions") {
        section("Define Custom Colors") {
            paragraph "Define colors with names and RGB values. These colors can be used in holiday configurations."
            input "colorCount", "number", title: "Number of Colors", range: "1..50", defaultValue: 10, submitOnChange: true
        }
        if (colorCount) {
            for (int i = 1; i <= colorCount; i++) {
                section("Color ${i}") {
                    input "colorName${i}", "text", title: "Color Name", required: false, description: "e.g., Red, Green, Orange"
                    input "colorR${i}", "number", title: "Red (0-255)", range: "0..255", required: false, defaultValue: 0
                    input "colorG${i}", "number", title: "Green (0-255)", range: "0..255", required: false, defaultValue: 0
                    input "colorB${i}", "number", title: "Blue (0-255)", range: "0..255", required: false, defaultValue: 0
                }
            }
        }
        section("Preset Colors (for reference)") {
            paragraph """
Common colors:
- Red: 255, 0, 0
- Green: 0, 255, 0
- Blue: 0, 0, 255
- White: 255, 255, 255
- Warm White: 255, 200, 150
- Orange: 255, 165, 0
- Purple: 128, 0, 128
- Yellow: 255, 255, 0
- Pink: 255, 105, 180
- Cyan: 0, 255, 255
"""
        }
    }

    // Holidays Page
    page(name: "holidaysPage", title: "Holiday Configurations") {
        section("Configure Holidays") {
            input "holidayCount", "number", title: "Number of Holidays", range: "1..20", defaultValue: 5, submitOnChange: true
        }
        if (holidayCount) {
            for (int i = 1; i <= holidayCount; i++) {
                section("Holiday ${i}") {
                    input "holidayName${i}", "text", title: "Holiday Name", required: false, description: "e.g., Christmas, Halloween"
                    input "holidayEnabled${i}", "bool", title: "Enabled", defaultValue: true
                    input "holidayStartMonth${i}", "enum", title: "Start Month", options: getMonthOptions(), required: false
                    input "holidayStartDay${i}", "number", title: "Start Day", range: "1..31", required: false
                    input "holidayEndMonth${i}", "enum", title: "End Month", options: getMonthOptions(), required: false
                    input "holidayEndDay${i}", "number", title: "End Day", range: "1..31", required: false
                    input "holidayPriority${i}", "number", title: "Priority (lower = higher priority)", range: "1..100", defaultValue: 50
                    href "holidayColorsPage", title: "Configure Colors for ${settings?.getAt("holidayName${i}") ?: "Holiday ${i}"}",
                        params: [holidayIndex: i], description: "Set colors for each day of the week"
                }
            }
        }
    }

    // Holiday Colors Page (per holiday, per day of week)
    page(name: "holidayColorsPage", title: "Holiday Color Configuration") {
        def holidayIndex = params?.holidayIndex ?: 1
        def holidayName = settings?.getAt("holidayName${holidayIndex}") ?: "Holiday ${holidayIndex}"
        def colorOptions = getDefinedColorOptions()

        section("Colors for ${holidayName}") {
            paragraph "Select colors for each day of the week. You can specify up to 3 colors that will be used in the WLED payload."
        }

        // Weekday colors (Monday - Friday)
        section("Weekday Colors (Monday - Friday)") {
            input "holiday${holidayIndex}WeekdayColor1", "enum", title: "Primary Color", options: colorOptions, required: false
            input "holiday${holidayIndex}WeekdayColor2", "enum", title: "Secondary Color", options: colorOptions, required: false
            input "holiday${holidayIndex}WeekdayColor3", "enum", title: "Tertiary Color (or Black for none)", options: colorOptions, required: false
        }

        // Weekend colors (Saturday - Sunday)
        section("Weekend Colors (Saturday - Sunday)") {
            input "holiday${holidayIndex}WeekendColor1", "enum", title: "Primary Color", options: colorOptions, required: false
            input "holiday${holidayIndex}WeekendColor2", "enum", title: "Secondary Color", options: colorOptions, required: false
            input "holiday${holidayIndex}WeekendColor3", "enum", title: "Tertiary Color (or Black for none)", options: colorOptions, required: false
        }

        // Or configure each day individually
        section("Advanced: Individual Day Configuration") {
            input "holiday${holidayIndex}UseIndividualDays", "bool", title: "Use Individual Day Configuration", defaultValue: false, submitOnChange: true
        }

        if (settings?.getAt("holiday${holidayIndex}UseIndividualDays")) {
            def days = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
            days.eachWithIndex { day, idx ->
                section("${day}") {
                    input "holiday${holidayIndex}Day${idx}Color1", "enum", title: "Primary Color", options: colorOptions, required: false
                    input "holiday${holidayIndex}Day${idx}Color2", "enum", title: "Secondary Color", options: colorOptions, required: false
                    input "holiday${holidayIndex}Day${idx}Color3", "enum", title: "Tertiary Color", options: colorOptions, required: false
                }
            }
        }
    }
}

def getMonthOptions() {
    return [
        "1": "January",
        "2": "February",
        "3": "March",
        "4": "April",
        "5": "May",
        "6": "June",
        "7": "July",
        "8": "August",
        "9": "September",
        "10": "October",
        "11": "November",
        "12": "December"
    ]
}

def getDefinedColorOptions() {
    def options = [:]
    options["black"] = "Black (Off)"

    if (colorCount && settings) {
        for (int i = 1; i <= colorCount; i++) {
            def name = settings["colorName${i}"]
            if (name) {
                def r = settings["colorR${i}"] ?: 0
                def g = settings["colorG${i}"] ?: 0
                def b = settings["colorB${i}"] ?: 0
                options["color${i}"] = "${name} (${r},${g},${b})"
            }
        }
    }
    return options
}

def getControllersDescription() {
    def count = controllerCount ?: 0
    def enabled = 0
    if (settings) {
        for (int i = 1; i <= count; i++) {
            if (settings["controllerEnabled${i}"] != false && settings["controllerEndpoint${i}"]) {
                enabled++
            }
        }
    }
    return "${enabled} controller(s) configured"
}

def getColorsDescription() {
    def count = 0
    if (colorCount && settings) {
        for (int i = 1; i <= colorCount; i++) {
            if (settings["colorName${i}"]) {
                count++
            }
        }
    }
    return "${count} color(s) defined"
}

def getHolidaysDescription() {
    def count = 0
    if (holidayCount && settings) {
        for (int i = 1; i <= holidayCount; i++) {
            if (settings["holidayName${i}"] && settings["holidayEnabled${i}"] != false) {
                count++
            }
        }
    }
    return "${count} holiday(s) configured"
}

def installed() {
    logDebug "WLED Holiday Lights Controller installed"
    initialize()
}

def updated() {
    logDebug "WLED Holiday Lights Controller updated"
    unschedule()
    initialize()
}

def uninstalled() {
    logDebug "WLED Holiday Lights Controller uninstalled"
    unschedule()
}

def initialize() {
    logDebug "Initializing WLED Holiday Lights Controller"

    if (enableSchedule) {
        scheduleJobs()
    }

    // Subscribe to button events
    subscribe(location, "button", buttonHandler)
}

def scheduleJobs() {
    if (turnOnTime) {
        def onTime = toDateTime(turnOnTime)
        schedule("0 ${onTime.format('mm')} ${onTime.format('HH')} * * ?", turnOnHandler)
        logDebug "Scheduled turn on at ${onTime.format('HH:mm')}"
    }

    if (turnOffTime) {
        def offTime = toDateTime(turnOffTime)
        schedule("0 ${offTime.format('mm')} ${offTime.format('HH')} * * ?", turnOffHandler)
        logDebug "Scheduled turn off at ${offTime.format('HH:mm')}"
    }
}

def turnOnHandler() {
    logDebug "Turn on handler triggered"
    triggerLights()
}

def turnOffHandler() {
    logDebug "Turn off handler triggered"
    turnOffLights()
}

def appButtonHandler(btn) {
    switch(btn) {
        case "manualTrigger":
            logDebug "Manual trigger button pressed"
            triggerLights()
            break
        case "manualOff":
            logDebug "Manual off button pressed"
            turnOffLights()
            break
    }
}

def triggerLights() {
    logDebug "Triggering lights..."

    def activeHoliday = getActiveHoliday()
    if (!activeHoliday) {
        log.info "No active holiday found for today"
        return
    }

    logDebug "Active holiday: ${activeHoliday.name}"

    // Turn on power switches if configured
    if (powerSwitches && powerSwitches.size() > 0) {
        log.info "Turning on ${powerSwitches.size()} power switch(es) for WLED controllers"
        powerSwitches.each { sw ->
            log.info "Turning on power switch: ${sw.displayName}"
            sw.on()
        }

        // Wait for WLED controllers to boot up before sending commands
        def delay = powerSwitchDelay ?: 20
        log.info "Waiting ${delay} seconds for WLED controllers to boot up"
        runIn(delay, "sendWledCommandsForHoliday", [data: [holidayIndex: activeHoliday.index, holidayName: activeHoliday.name]])
    } else {
        // No power switches configured, send WLED commands immediately
        sendWledCommandsForHoliday([holidayIndex: activeHoliday.index, holidayName: activeHoliday.name])
    }
}

def sendWledCommandsForHoliday(data) {
    def holidayIndex = data.holidayIndex
    def holidayName = data.holidayName

    log.info "Sending WLED commands for holiday: ${holidayName}"

    def holiday = [index: holidayIndex, name: holidayName]
    def colors = getColorsForToday(holiday)
    logDebug "Colors for today: ${colors}"

    def payload = buildPayload(colors)
    logDebug "Payload: ${payload}"

    sendToControllers(payload)
}

def turnOffLights() {
    log.info "Turning off WLED lights"

    def offPayload = '{"on":false}'
    sendToControllers(offPayload)

    // Turn off power switches if configured
    if (turnOffPowerSwitches && powerSwitches && powerSwitches.size() > 0) {
        log.info "Turning off ${powerSwitches.size()} power switch(es)"
        powerSwitches.each { sw ->
            log.info "Turning off power switch: ${sw.displayName}"
            sw.off()
        }
    }
}

def getActiveHoliday() {
    def today = new Date()
    def currentMonth = today.format('M').toInteger()
    def currentDay = today.format('d').toInteger()

    logDebug "Checking holidays for date: ${currentMonth}/${currentDay}"

    def activeHolidays = []

    if (holidayCount) {
        for (int i = 1; i <= holidayCount; i++) {
            def name = settings["holidayName${i}"]
            def enabled = settings["holidayEnabled${i}"] != false
            def startMonth = settings["holidayStartMonth${i}"]?.toInteger()
            def startDay = settings["holidayStartDay${i}"]
            def endMonth = settings["holidayEndMonth${i}"]?.toInteger()
            def endDay = settings["holidayEndDay${i}"]
            def priority = settings["holidayPriority${i}"] ?: 50

            if (name && enabled && startMonth && startDay && endMonth && endDay) {
                if (isDateInRange(currentMonth, currentDay, startMonth, startDay, endMonth, endDay)) {
                    activeHolidays << [
                        index: i,
                        name: name,
                        priority: priority
                    ]
                    logDebug "Holiday '${name}' is active"
                }
            }
        }
    }

    if (activeHolidays.isEmpty()) {
        return null
    }

    // Return holiday with lowest priority number (highest priority)
    activeHolidays.sort { it.priority }
    return activeHolidays[0]
}

def isDateInRange(currentMonth, currentDay, startMonth, startDay, endMonth, endDay) {
    def current = currentMonth * 100 + currentDay
    def start = startMonth * 100 + startDay
    def end = endMonth * 100 + endDay

    // Handle year wrap-around (e.g., December to January)
    if (start <= end) {
        // Normal range within same year
        return current >= start && current <= end
    } else {
        // Range wraps around year (e.g., Dec 1 to Jan 10)
        return current >= start || current <= end
    }
}

def getColorsForToday(holiday) {
    def today = new Date()
    def dayOfWeek = today.format('u').toInteger() // 1 = Monday, 7 = Sunday
    def dayIndex = (dayOfWeek % 7) // Convert to 0 = Sunday, 6 = Saturday

    def holidayIndex = holiday.index
    def colors = []

    // Check if using individual day configuration
    if (settings["holiday${holidayIndex}UseIndividualDays"]) {
        colors << getColorRGB(settings["holiday${holidayIndex}Day${dayIndex}Color1"])
        colors << getColorRGB(settings["holiday${holidayIndex}Day${dayIndex}Color2"])
        colors << getColorRGB(settings["holiday${holidayIndex}Day${dayIndex}Color3"])
    } else {
        // Use weekday/weekend configuration
        def isWeekend = (dayIndex == 0 || dayIndex == 6) // Sunday or Saturday

        if (isWeekend) {
            colors << getColorRGB(settings["holiday${holidayIndex}WeekendColor1"])
            colors << getColorRGB(settings["holiday${holidayIndex}WeekendColor2"])
            colors << getColorRGB(settings["holiday${holidayIndex}WeekendColor3"])
        } else {
            colors << getColorRGB(settings["holiday${holidayIndex}WeekdayColor1"])
            colors << getColorRGB(settings["holiday${holidayIndex}WeekdayColor2"])
            colors << getColorRGB(settings["holiday${holidayIndex}WeekdayColor3"])
        }
    }

    // Replace null colors with black [0,0,0]
    colors = colors.collect { it ?: [0, 0, 0] }

    return colors
}

def getColorRGB(colorKey) {
    if (!colorKey || colorKey == "black") {
        return [0, 0, 0]
    }

    if (colorKey.startsWith("color")) {
        def index = colorKey.replace("color", "").toInteger()
        def r = settings["colorR${index}"] ?: 0
        def g = settings["colorG${index}"] ?: 0
        def b = settings["colorB${index}"] ?: 0
        return [r, g, b]
    }

    return [0, 0, 0]
}

def buildPayload(colors) {
    def payload = basePayload

    if (!payload) {
        log.error "No base payload configured"
        return null
    }

    try {
        def json = new groovy.json.JsonSlurper().parseText(payload)

        // Replace colors in the seg array
        if (json.seg && json.seg.size() > 0) {
            def seg = json.seg[0]
            if (seg.col && seg.col.size() >= 3) {
                // Replace the first 3 colors with our holiday colors
                for (int i = 0; i < Math.min(3, colors.size()); i++) {
                    seg.col[i] = colors[i]
                }
            }
        }

        // Turn on the lights
        json.on = true

        return new groovy.json.JsonBuilder(json).toString()
    } catch (Exception e) {
        log.error "Error building payload: ${e.message}"
        return payload
    }
}

def sendToControllers(payload) {
    if (!payload) {
        log.error "No payload to send"
        return
    }

    if (controllerCount) {
        for (int i = 1; i <= controllerCount; i++) {
            def enabled = settings["controllerEnabled${i}"] != false
            def endpoint = settings["controllerEndpoint${i}"]
            def name = settings["controllerName${i}"] ?: "Controller ${i}"

            if (enabled && endpoint) {
                log.info "Sending POST to ${name} at ${endpoint}"
                sendPost(endpoint, payload, name)
            }
        }
    }
}

def sendPost(endpoint, payload, controllerName) {
    try {
        def params = [
            uri: endpoint,
            contentType: "application/json",
            body: payload,
            timeout: 10
        ]

        asynchttpPost("postResponseHandler", params, [controllerName: controllerName])
        logDebug "POST sent to ${controllerName}"

    } catch (Exception e) {
        log.error "Error sending POST to ${controllerName}: ${e.message}"
    }
}

def postResponseHandler(response, data) {
    def controllerName = data?.controllerName ?: "Unknown"

    if (response.hasError()) {
        log.error "Error response from ${controllerName}: ${response.getErrorMessage()}"
    } else {
        log.info "Success response from ${controllerName} (HTTP ${response.status})"
    }
}

def logDebug(msg) {
    if (enableDebugLog) {
        log.debug msg
    }
}
