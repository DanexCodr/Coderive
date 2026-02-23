const strings = {
  ui: {
    titles: {
      coderive_ide: "Coderive IDE",
      coderive_language: "Coderive",
      tagline: "A modern, expressive programming language with interpreter and compiler capabilities",
      main_page: "Coderive - Modern Programming Language"
    },
    buttons: {
      code: "Code",
      output: "Output",
      get_started: "Get Started",
      try_online_editor: "Try Coderive",
      view_on_github: "View on GitHub",
      open_online_editor: "Open Online Editor",
      copy: "Copy",
      copied: "Copied!",
      menu: "☰",
      close: "✕"
    },
    navigation: {
      coderive_files: "Coderive Files",
      projects: "Projects",
      examples: "Examples"
    },
    menu_items: {
      main_cod: "main.cod",
      utils_cod: "utils.cod",
      config_cod: "config.cod",
      myapp: "MyApp",
      demo_project: "DemoProject",
      test_suite: "TestSuite",
      hello_world: "Hello World",
      calculator: "Calculator",
      data_structures: "Data Structures"
    },
    placeholders: {
      code_editor: "// Welcome to Coderive IDE\n// Start typing your Coderive code here...\n\nout(\"Hello, Coderive!\")",
      output_simulation: "Coderive Output:\nRunning code...\nHello, Coderive!\nExecution completed successfully.\nTime: "
    },
    labels: {
      features: "Key Features",
      language_examples: "Language Examples",
      execution_runners: "Execution Runners",
      technical_innovations: "Technical Innovations",
      getting_started: "Getting Started",
      try_online: "Try Coderive Online",
      demo_title: "InteractiveDemo.cod - Full Example"
    },
    messages: {
      try_description: "Experience Coderive right in your browser with our online editor. No installation required.",
      footer_built_with: "Built with passion and persistence on mobile devices — proving that innovation knows no hardware boundaries.",
      copyright: "© 2026 Coderive Project. All rights reserved."
    }
  },
  content: {
    features: [
      {
        name: "Dual Execution Modes",
        description: "Run code immediately with the interpreter or compile to bytecode and native assembly for performance.",
        icon: "⚙️"
      },
      {
        name: "Advanced Linting",
        description: "Built-in static analysis to catch potential issues before execution with configurable linting rules.",
        icon: "🔍"
      },
      {
        name: "Interactive REPL",
        description: "Experiment with code in real-time using the interactive Read-Eval-Print-Loop environment.",
        icon: "💬"
      },
      {
        name: "Performance Optimized",
        description: "MTOT (Multi-Target Optimization Technology) compilation for efficient native code generation.",
        icon: "⚡"
      },
      {
        name: "Mobile-First Development",
        description: "Built entirely on mobile devices, proving serious development can happen anywhere.",
        icon: "📱"
      }
    ],
    runners: [
      {
        name: "TestRunner",
        description: "Execute Coderive code directly with hardcoded paths for faster testing cycles.",
        command: "java -cp coderive.jar cod.runner.TestRunner program.cod",
        features: [
          "Immediate code execution",
          "Built-in debugging support"
        ]
      },
      {
        name: "CompilerTestRunner",
        description: "Compile Coderive code to ir bytecode or native assembly for deployment.",
        command: "java -cp coderive.jar cod.runner.CompilerTestRunner --native program.cod",
        features: [
          "Bytecode compilation",
          "Native assembly output",
          "MTOT optimization"
        ]
      },
      {
        name: "CommandRunner",
        description: "Unified runner with multiple execution modes in a single command.",
        command: "java -cp coderive.jar cod.runner.CommandRunner -c program.cod",
        features: [
          "Interpret or compile",
          "Flexible out(options)",
          "All features in one tool"
        ]
      },
      {
        name: "REPLRunner",
        description: "Interactive environment for experimenting with Coderive code.",
        command: "java -cp coderive.jar cod.runner.REPLRunner",
        features: [
          "Real-time evaluation",
          "Multi-line input support",
          "State management"
        ]
      }
    ],
    getting_started: [
      {
        step: "1. Installation",
        description: "Clone the repository and build the project:",
        command: "git clone https://github.com/DanexCodr/Coderive.git\ncd Coderive\n./gradlew build"
      },
      {
        step: "2. Write Your First Program",
        description: "Create a simple Coderive program:",
        code_example: "out(\"Hello, Coderive!\")"
      },
      {
        step: "3. Run Your Code",
        description: "Execute your program with the interpreter:",
        command: "java -cp build/libs/coderive.jar cod.runner.InterpreterRunner hello.cod"
      }
    ],
    technical_innovations: [
      {
        title: "Mobile-First Architecture",
        description: "Built entirely on Android devices using Java NIDE, Quickedit, and Termux, proving serious development can happen outside traditional environments."
      },
      {
        title: "Advanced Constructor Resolution",
        description: "Intelligent constructor matching with named/positional arguments, default values, and inheritance chain validation. Prevents circular inheritance and ensures proper initialization."
      },
      {
        title: "Viral Policy System",
        description: "Unique policy-based programming where policies are viral through inheritance. Ancestor policies automatically propagate to descendant classes, enforcing method implementations."
      },
      {
        title: "Smart For-Loop Pattern Recognition",
        description: "Advanced pattern matching in loops detects conditional assignments, sequence patterns, and optimizes array operations at parse-time for runtime efficiency."
      },
      {
        title: "Type System with Union Types",
        description: "Sophisticated type handling supporting union types (Int|none), array wildcards ([]), and automatic type inference with validation at multiple scoping levels."
      }
    ],
    footer: {
      copyright: "© 2026 Coderive Project. All rights reserved.",
      built_with: "Built with passion and persistence on mobile devices — proving that innovation knows no hardware boundaries.",
      links: {
        github: "GitHub",
        features: "Features",
        documentation: "Documentation",
        contact: "Contact"
      }
    }
  },
  metadata: {
    project: "Coderive",
    author: "DanexCodr",
    github: "https://github.com/DanexCodr/Coderive",
    contact_email: "danisonnunez001@gmail.com",
    extraction_date: "2026-01-22"
  }
};