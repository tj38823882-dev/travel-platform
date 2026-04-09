<script setup>

import { ref } from 'vue'
import draggable from 'vuedraggable'

// 子 接收父 的資料
// 傳資料的是父元件用 <Child :xxx="yyy" />
const props = defineProps({
    points: { type: Array, required: true },
    selectedPointId: { type: Number, default: null },
    arrivalTimes: { type: Array, required: true },
    startTime: { type: String, required: true },
    travelModes: { type: Array, required: true },
    readOnly: { type: Boolean, default: false }
})

const emit = defineEmits([
    'update:points',
    'update:startTime',
    'reorder',
    'select',
    'remove',
    'rename',
    'update-stay',
    'update-travel-mode',
    'update-travel-minutes',
    'update-note'
])



/* ---- 拖曳狀態 ---- */
const drag = ref(false)

function onDragEnd() {
    drag.value = false
    emit('reorder')
}


/* ---- 展開 TRANSIT 明細的 point id set ---- */
const expandedSteps = ref(new Set())

function toggleSteps(pointId) {
    const s = new Set(expandedSteps.value)
    if (s.has(pointId)) s.delete(pointId)
    else s.add(pointId)
    expandedSteps.value = s
}



/* ---- 備註有關 下拉選單開關 ---- */
const openMenuId = ref(null)

function toggleMenu(pointId) {
    openMenuId.value = openMenuId.value === pointId ? null : pointId
}

function closeMenu() {
    openMenuId.value = null
}

/* ---- 備註 popover 開關 ---- */
const openNoteId = ref(new Set())
// Set，因為你的操作是「toggle 某個 id 的開關」，需要快速判斷某個 id 是否存在（has()），Set
function toggleNote(pointId) {
    const s = new Set(openNoteId.value)
    if (s.has(pointId)) s.delete(pointId)
    else s.add(pointId)
    openNoteId.value = s
    closeMenu()
}


</script>


<template>
    <!-- 行程列表 -->
    <div class="itinerary-list">

        <div v-if="points.length === 0" class="empty-hint">
            點擊地圖或搜尋來新增地點
        </div>

        <!-- <draggable v-model="points" item-key="id" handle=".drag-handle" ghost-class="drag-ghost" @start="drag = true"
            @end="onDragEnd"> -->
        <!-- 完整寫法是 <template v-slot:item> 其實就是 它就是一個名字叫 item 的 slot  -->
        <!-- 有兩個參數 分別是 element (當前項目資料) 和 index (當前的陣列位置) 冒號只是把它又重新命名， 在這個case 他會去跑 points 陣列，然後一筆一筆把 element 跟 index 傳進來-->

        <draggable :modelValue="points" @update:modelValue="emit('update:points', $event)" item-key="id"
            handle=".drag-handle" ghost-class="drag-ghost" @start="drag = true" @end="onDragEnd">

            <template #item="{ element: point, index }">

                <div class="itinerary-item-wrapper">

                    <!-- ---- 行程行 ---- -->
                    <div class="itinerary-row" :class="{ 'is-selected': selectedPointId === point.id }"
                        @click="emit('select', point)">

                        <div class="drag-handle">⠿</div>

                        <!-- 左側：縮圖 + badge -->
                        <div class="item-left">
                            <div class="row-badge">{{ index + 1 }}</div>
                            <img v-if="point.photos?.length" :src="point.photos[0]" :alt="point.name"
                                class="row-thumb" />
                            <div v-else class="row-thumb row-thumb-empty">
                                <i class="bi bi-geo-alt"></i>
                            </div>
                        </div>

                        <!-- 右側：時間 + 名稱 + 刪除 -->
                        <div class="item-right">
                            <div class="time-row">
                                <i class="bi bi-clock-fill time-icon"></i>
                                <!-- index 0 可編輯，其他用 computed 顯示 -->
                                <input type="time" :value="index === 0 ? startTime : arrivalTimes[index]"
                                    class="arrival-time-input" :readonly="index !== 0" lang="zh-TW"
                                    @change=" index === 0 ? emit('update:startTime', $event.target.value) : null"
                                    @click.stop="index === 0 ? $event.target.showPicker() : null" />
                            </div>
                            <input type="text" class="row-name" :value="point.name || point.address || '未命名地點'" readonly
                                @change="emit('rename', point.id, $event.target.value)" @click.stop />



                            <!-- Set，因為你的操作是「toggle 某個 id 的開關」，需要快速判斷某個 id 是否存在（has()） -->
                            <input v-if="(openNoteId.has(point.id) || point.note)" type="text" class="row-note"
                                :value="point.note || ''" placeholder="新增備註..."
                                @input="emit('update-note', point.id, $event.target.value)"
                                @keydown.enter="$event.target.blur()" @click.stop />
                        </div>

                        <!-- <button class="row-more-btn" @click.stop="emit('remove', point.id)">✕</button> -->
                        <!-- 三個點按鈕 -->
                        <div v-if="!readOnly" class="row-menu-wrapper" @click.stop>
                            <button class="row-more-btn" @click="toggleMenu(point.id)">
                                <i class="bi bi-three-dots"></i>
                            </button>

                            <!-- 下落選單 -->
                            <div v-if="openMenuId === point.id" class="row-dropdown">
                                <button class="dropdown-item dropdown-item--note" @click="toggleNote(point.id)">
                                    <i class="bi bi-pencil-square"></i> 備註
                                </button>
                                <div class="dropdown-divider"></div>
                                <button class="dropdown-item dropdown-item--delete"
                                    @click="emit('remove', point.id); closeMenu()">
                                    <i class="bi bi-trash3"></i> 刪除
                                </button>
                            </div>
                        </div>
                    </div>



                    <!-- ---- 停留時間（最後一個不顯示） ---- -->
                    <div v-if="index < points.length - 1" class="stay-row">
                        <span class="stay-label">🕐 停留</span>
                        <input type="number" min="0" :value="point.stayMinutes" class="stay-input"
                            @input="emit('update-stay', point.id, Number($event.target.value))" />
                        <span class="stay-unit">分鐘</span>
                    </div>

                    <!-- ---- gap-row：交通方式（最後一個不顯示） 裡面塞各式交通---- -->
                    <div v-if="index < points.length - 1" class="gap-section">
                        <div class="timeline-line"></div>

                        <div class="gap-row">
                            <!-- 交通模式按鈕 -->
                            <div class="mode-btns">
                                <button v-for="mode in travelModes" :key="mode.value" class="mode-btn"
                                    :class="{ 'is-active': point.travelMode === mode.value }" :title="mode.title"
                                    @click="emit('update-travel-mode', point.id, mode.value)">
                                    {{ mode.label }}
                                </button>
                            </div>

                            <!-- CUSTOM：手動輸入交通時間 -->
                            <template v-if="point.travelMode === 'CUSTOM'">
                                <input type="number" min="0" :value="point.travelMinutes" class="gap-input"
                                    @input="emit('update-travel-minutes', point.id, Number($event.target.value))" />
                                <span class="gap-unit">分鐘</span>
                            </template>

                            <!-- 非 CUSTOM：顯示 API 估出的時間 -->
                            <template v-else>
                                <span class="gap-estimated">
                                    {{ point.travelMinutes ? `${point.travelMinutes} 分鐘` : '查詢中...' }}
                                </span>
                            </template>

                            <!-- TRANSIT：展開明細按鈕 -->
                            <button v-if="point.travelMode === 'TRANSIT' && point.routeSteps?.length"
                                class="steps-toggle" @click="toggleSteps(point.id)">
                                {{ expandedSteps.has(point.id) ? '▲' : '▼' }}
                            </button>
                        </div>

                        <!-- TRANSIT 分段明細 -->
                        <div v-if="point.travelMode === 'TRANSIT' && expandedSteps.has(point.id) && point.routeSteps?.length"
                            class="transit-steps">
                            <div v-for="(step, si) in point.routeSteps" :key="si" class="transit-step">
                                <template v-if="step.mode === 'TRANSIT'">
                                    <span class="step-icon">{{ step.transit.vehicleIcon }}</span>
                                    <div class="step-body">
                                        <div class="step-line" :style="{ borderColor: step.transit.lineColor }">
                                            {{ step.transit.lineName }}
                                        </div>
                                        <div class="step-stops">
                                            {{ step.transit.departureStop }} ➡️ {{ step.transit.arrivalStop }}
                                            <span class="step-num-stops">（{{ step.transit.numStops }} 站）</span>
                                        </div>
                                        <div class="step-duration">{{ step.duration }} 分鐘</div>
                                    </div>
                                </template>
                                <template v-else>
                                    <span class="step-icon">🚶</span>
                                    <div class="step-body">
                                        <div class="step-walk">步行 {{ step.duration }} 分鐘（{{ step.distance }}）</div>
                                    </div>
                                </template>
                            </div>
                        </div>

                    </div>

                </div>
            </template>
        </draggable>

    </div>
</template>



<style scoped>
.itinerary-list {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overscroll-behavior: contain;
}

.itinerary-list::-webkit-scrollbar {
    width: 6px;
}

.itinerary-list::-webkit-scrollbar-track {
    background: transparent;
}

.itinerary-list::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 10px;
}

.empty-hint {
    color: #9ca3af;
    font-size: 15px;
    font-weight: 500;
    text-align: center;
    padding: 60px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
}

.empty-hint::before {
    content: '📍';
    font-size: 40px;
    opacity: 0.5;
}

.itinerary-item-wrapper {
    margin-bottom: 8px;
    animation: slide-in 0.3s ease-out forwards;
}

@keyframes slide-in {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.itinerary-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px;
    background: #f1f4f9;
    border: 1px solid transparent;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    position: relative;
}

.itinerary-row:hover {
    background: #e8ecf3;
}

.itinerary-row.is-selected {
    border-color: #ff1b6b;
    background-color: #ffffff;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
}

.drag-handle {
    cursor: grab;
    color: #cbd5e1;
    font-size: 20px;
    padding-right: 3px;
    user-select: none;
    flex-shrink: 0;
}

.drag-handle:active {
    cursor: grabbing;
}

.row-id {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background: #ff1b6b;
    color: white;
    font-size: 13px;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.row-thumb {
    width: 60px;
    height: 42px;
    border-radius: 12px;
    object-fit: cover;
    flex-shrink: 0;
}

.row-thumb-empty {
    width: 72px;
    height: 72px;
    border-radius: 12px;
    background: #dae0ea;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #94a3b8;
    flex-shrink: 0;
}

.time-input {
    background: transparent;
    border: none;
    color: #ff1b6b;
    font-weight: 800;
    font-size: 14px;
    width: 80px;
    outline: none;
    cursor: pointer;
    padding: 0;
    font-family: inherit;
    flex-shrink: 0;
}

.time-input::-webkit-calendar-picker-indicator {
    display: none;
}

.time-input:focus {
    background: rgba(255, 27, 107, 0.05);
    border-radius: 4px;
}

.row-name {
    flex: 1;
    min-width: 0;
    border: none;
    background: transparent;
    font-size: 16px;
    font-weight: 600;
    color: #2c3e50;
    outline: none;
    padding: 4;
    font-family: inherit;
    width: 100%;

}

.row-name:focus {
    background: rgba(0, 0, 0, 0.03);
    border-radius: 4px;
    padding: 0px;
}

.row-delete {
    background: #dae0ea;
    border: none;
    color: #94a3b8;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 12px;
    flex-shrink: 0;
    transition: all 0.2s;
}

.row-delete:hover {
    background: #fee2e2;
    color: #ef4444;
    transform: scale(1.1);
}

/* 停留時間列 */
.stay-row {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 20px;
    padding: 4px 80px;
    font-size: 12px;
    color: #94a3b8;
}

.stay-label {
    font-size: 12px;
    color: #94a3b8;
}

.stay-input {
    width: 70px;
    padding: 2px 0px;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    font-size: 12px;
    text-align: center;
    outline: none;
}

.stay-input:focus {
    border-color: #7c3aed;
}

.stay-unit {
    color: #94a3b8;
}

/* gap section */
.gap-section {
    padding: 0px 0px 0px 0px;
    position: relative;
    /* ← 讓虛線以 gap-section 為基準 */

}

.gap-row {
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;
    padding: 6px 12px 0px 80px;
    background: rgba(255, 255, 255, 0.6);
    /* backdrop-filter: blur(4px); */
    border-radius: 30px;
    /* border: 1px solid rgba(226, 232, 240, 0.8); */
    font-size: 13px;
    font-weight: 700;
    color: #4a4769;
}

.mode-btns {
    display: flex;
    gap: 4px;
}

.mode-btn {
    background: #f1f5f9;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 4px 8px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.15s;
    line-height: 1;
}

.mode-btn:hover {
    background: #e2e8f0;
}

.mode-btn.is-active {
    background: #fff0f6;
    border-color: #ff1b6b;
}

.gap-input {
    width: 60px;
    height: 24px;
    padding: 2px 6px;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    font-size: 12px;
    text-align: center;
    outline: none;
    background: transparent;
    /* ↓ 改成跟 gap-estimated 一樣 */
    color: #ff1b6b;
    font-weight: 700;
    font-size: 13px;
    font-family: inherit;
    /* ← 繼承字體 */
}

.gap-input:focus {
    background: #ffffff;
    box-shadow: 0 0 0 2px rgba(255, 27, 107, 0.2);
}

.gap-unit {
    color: #94a3b8;
    font-size: 12px;
}

.gap-estimated {
    color: #ff1b6b;
    font-weight: 700;
    font-size: 13px;
    min-width: 55px;
    text-align: center;
}

.steps-toggle {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 10px;
    color: #94a3b8;
    padding: 2px 4px;
}

.steps-toggle:hover {
    color: #ff1b6b;
}

/* TRANSIT 分段明細 */
.transit-steps {
    margin: 15px 2px 10px 80px;
    background: #f8faff;
    border: 1px solid #e0e7ff;
    border-radius: 12px;
    padding: 8px 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.transit-step {
    display: flex;
    gap: 8px;
    align-items: flex-start;
}

.step-icon {
    font-size: 16px;
    flex-shrink: 0;
    margin-top: 1px;
}

.step-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 12px;
}

.step-line {
    display: inline-block;
    border-left: 3px solid #ff1b6b;
    padding-left: 6px;
    font-weight: 600;
    color: #333;
}

.step-stops {
    color: #555;
}

.step-num-stops {
    color: #94a3b8;
}

.step-duration {
    color: #ff1b6b;
    font-size: 11px;
}

.step-walk {
    color: #666;
}

.drag-ghost {
    opacity: 0.4;
    background: #f8fafc;
    transform: scale(0.98);
    border-color: #7c3aed !important;
}



/* 左側：縮圖 + badge */
.item-left {
    position: relative;
    width: 100px;
    height: 90px;
    flex-shrink: 0;
}

.row-badge {
    position: absolute;
    top: 6px;
    left: -4px;
    background: #ff1b6b;
    color: white;
    padding: 2px 19px 2px 15px;
    font-weight: 800;
    font-size: 13px;
    z-index: 5;
    clip-path: polygon(0% 0%, 100% 0%, 85% 50%, 100% 100%, 0% 100%);
}

.row-thumb {
    width: 100%;
    height: 100%;
    border-radius: 12px;
    object-fit: cover;
}

.row-thumb-empty {
    width: 100%;
    height: 100%;
    border-radius: 12px;
    background: #dae0ea;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #94a3b8;
}

/* 右側：時間 + 名稱 */
.item-right {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;
}

.time-row {
    display: flex;
    align-items: center;
    gap: 4px;
}

.time-icon {
    color: #ff1b6b;
    font-size: 14px;
    flex-shrink: 0;
}

.arrival-time-input {
    background: transparent;
    border: none;
    color: #ff1b6b;
    font-weight: 800;
    font-size: 22px;
    outline: none;
    cursor: pointer;
    padding: 0;
    font-family: inherit;
    width: 150px;
    flex-shrink: 0;


}

.arrival-time-input::-webkit-calendar-picker-indicator {
    display: none;
}

/* 右上角刪除按鈕 * */

/* .row-more-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    background: #dae0ea;
    border: none;
    color: #94a3b8;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 12px;
    flex-shrink: 0;
    align-self: flex-start;
    transition: all 0.2s;
}

.row-more-btn:hover {
    background: #fee2e2;
    color: #ef4444;
    transform: scale(1.1);
} */




.timeline-line {
    position: absolute;
    left: 60px;
    top: -32px;
    bottom: -12px;
    width: 1px;
    border-left: 1px dashed #cbd5e1;
}





/* 備註相關 */

/* ===== 三點選單 ===== */
.row-menu-wrapper {
    position: absolute;
    top: 8px;
    right: 8px;
    z-index: 10;
}

.row-more-btn {
    background: #dae0ea;
    border: none;
    color: #94a3b8;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.row-more-btn:hover {
    background: #cbd5e1;
    color: #475569;
}

.row-dropdown {
    position: absolute;
    top: 34px;
    right: 0;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    min-width: 120px;
    overflow: hidden;
    z-index: 100;
}

.dropdown-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 10px 14px;
    background: none;
    border: none;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    text-align: left;
    transition: background 0.15s;
}

.dropdown-item:hover {
    background: #f8fafc;
}

.dropdown-item--note {
    color: #7c3aed;
}

.dropdown-item--note:hover {
    background: #f5f3ff;
}

.dropdown-item--delete {
    color: #ef4444;
}

.dropdown-item--delete:hover {
    background: #fef2f2;
}

.dropdown-divider {
    height: 1px;
    background: #f1f5f9;
}

.row-note {
    border: none;
    background: transparent;
    font-size: 12px;
    font-weight: 400;
    color: #94a3b8;
    outline: none;
    padding: 0;
    font-family: inherit;
    width: 100%;
}

.row-note::placeholder {
    color: #cbd5e1;
}

.row-note:focus {
    color: #475569;
}
</style>