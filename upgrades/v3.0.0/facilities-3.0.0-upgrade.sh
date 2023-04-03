#!/bin/bash

# Fixes an illegal byte sequence issue on MacOS
export LC_ALL=C

function usage {
  echo "Usage: $0 [directory] [-d]"
  echo
  echo "Where:"
  echo "   -d perform a dry run (default: true)"
  echo
  echo "Example 1 (dry run):"
  echo "   $0 /some/cool/directory"
  echo "Example 2 (live run):"
  echo "   $0 /some/cool/directory -d false"
  exit 0
}

if [ -z "$1" ]; then
  usage
else
  ROOT_DIR=$1
  shift
fi

DRY_RUN=true

while getopts ":d:" OPTION; do
  case $OPTION in
  d)
    if [ "$OPTARG" = false ] || [ "$OPTARG" = 0 ]; then
      DRY_RUN=false
    elif [ "$OPTARG" = true ] || [ "$OPTARG" = 1 ]; then
      DRY_RUN=true
    else
      echo "Invalid option passed to -d: $OPTARG"
      echo -e "Acceptable values are: true, false, 1, 0\n"
      usage
      exit 1
    fi
    ;;
  \?)
    usage
    exit 1
    ;;
  :)
    usage
    exit 1
    ;;
  esac
done

if $DRY_RUN; then
  echo "Doing a dry run. No files will be modified."
else
  echo "Doing a live run. Files may be modified."
fi

# Find & Replace all imports
function process_file {
  local file=$1
  local yaml_mappings=(
    "com.mx.redis([^a-zA-Z])=com.mx.path.service.facility.store.redis\1"
    "com.mx.path.facility.store.vault([^a-zA-Z])=com.mx.path.service.facility.store.vault\1"
    "com.mx.messaging.nats([^a-zA-Z])=com.mx.path.service.facility.messaging.nats\1"
    "com.mx.path.service.facility.fault_tolerant_executor.Resilience4jFaultTolerantExecutor=com.mx.path.service.facility.fault_tolerant_executor.resilience4j.Resilience4jFaultTolerantExecutor"
    "com.mx.path.facility.exception_reporter.honeybadger([^a-zA-Z])=com.mx.path.service.facility.exception_reporter.honeybadger\1"
    "com.mx.path.facility.security.vault([^a-zA-Z])=com.mx.path.service.facility.security.vault\1"
    "connectionTimeout([^a-zA-Z])=timeout\1"
  )

  local reported_file=false
  local mappings=("${global_mappings[@]}")

  if [ "${file: -5}" == ".yaml" ] || [ "${file: -5}" == ".yml" ]; then
    mappings=("${mappings[@]}" "${yaml_mappings[@]}")
  else
    echo "Skipping non-yaml file $file"
  fi

  for pair in "${mappings[@]}"; do
    local from="${pair%%=*}"
    local to="${pair#*=}"

    changes="$(sed <"$file" -E -n "s/$from/$to/gp")"
    if [ -n "$changes" ]; then
      if ! $reported_file; then
        echo
        echo "Found match(es) in file: $file"
        reported_file=true
      fi
      echo "Changing \"$from\" to \"$to\", resulting in:"
      echo "$changes"
      if ! $DRY_RUN; then
        sed -E -i "" "s/$from/$to/g" "$file"
      fi
    fi
  done
  if $reported_file; then
    echo
    echo
  fi
}

### Driver
export -f process_file
export DRY_RUN
find "$ROOT_DIR" -type f \( -iname "*.yaml" -or -iname "*.yml" \) -exec bash -c 'process_file "$0"' {} \;
exit 0