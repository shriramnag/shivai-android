<<<< REPLACE
        register(QuarantineTool(securityAgent))
    }

    val taskExecutor = TaskExecutor(toolRegistry, stopController, safetyEngine)
==== WITH
        register(QuarantineTool(securityAgent))
        // Extended Tools
        register(WebResearchTool(webResearchAgent))
        register(FileAgentTool(fileAgent))
        register(TermuxTool(termuxBridge))
        register(SystemHealthTool(systemHealthMonitor, permissionIntelligence))
    }

    val taskExecutor = TaskExecutor(toolRegistry, stopController, safetyEngine)
    val missionManager = MissionManager(toolRegistry, safetyEngine, stopController, memoryDao = memoryDao)
>>>>
