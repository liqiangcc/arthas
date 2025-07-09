package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.TimeModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * View for TimeCommand result
 *
 * @author example
 */
public class TimeView extends ResultView<TimeModel> {

    @Override
    public void draw(CommandProcess process, TimeModel result) {
        if (result.isShowTimezone()) {
            // Show detailed time and timezone information
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

            table.row(label("Current Time").style(Decoration.bold.bold()),
                     label(result.getCurrentTime()));

            table.row(label("Timezone ID").style(Decoration.bold.bold()),
                     label(result.getTimezoneId()));

            table.row(label("Timezone Name").style(Decoration.bold.bold()),
                     label(result.getTimezoneName()));

            process.write(RenderUtil.render(table, process.width()));
        } else {
            // Show simple time
            LabelElement timeLabel = label(result.getCurrentTime())
                    .style(Decoration.bold.fg(Color.green));
            process.write(RenderUtil.render(timeLabel, process.width()));
        }
        process.write("\n");
    }
}
